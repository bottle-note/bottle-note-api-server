package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.alcohols.domain.QCurationKeyword.curationKeyword;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.alcohols.domain.QTastingTag.tastingTag;
import static app.bottlenote.global.search.SearchKeywordLikePattern.ESCAPE;
import static app.bottlenote.global.search.SearchKeywordLikePattern.contains;

import app.bottlenote.alcohols.constant.AdminAlcoholSortType;
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.search.AlcoholSearchTokenizer;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlcoholQuerySupporter {

  private static final Pattern KOREAN_NAME_TOKEN = Pattern.compile("[가-힣]{2,}");

  private final app.bottlenote.alcohols.domain.RegionRepository regionRepository;

  /** 주류에 연결된 테이스팅 태그 목록을 문자열로 조회 */
  public static Expression<String> getTastingTags() {
    return ExpressionUtils.as(
        JPAExpressions.select(Expressions.stringTemplate("group_concat({0})", tastingTag.korName))
            .from(alcoholsTastingTags)
            .join(tastingTag)
            .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
            .where(alcoholsTastingTags.alcohol.id.eq(alcohol.id)),
        "alcoholsTastingTags");
  }

  /** 주어진 주류가 인기 순위에 포함되는지 확인 */
  public BooleanExpression isHot5(NumberPath<Long> id, Collection<Long> hotAlcoholIds) {
    return hotAlcoholIds.isEmpty() ? Expressions.asBoolean(false) : id.in(hotAlcoholIds);
  }

  /** Admin용 정렬 조건 생성 (이름/카테고리 기준) */
  public OrderSpecifier<?> sortByAdmin(AdminAlcoholSortType sortType, SortOrder sortOrder) {
    return switch (sortType) {
      case KOR_NAME -> sortOrder == SortOrder.DESC ? alcohol.korName.desc() : alcohol.korName.asc();
      case ENG_NAME -> sortOrder == SortOrder.DESC ? alcohol.engName.desc() : alcohol.engName.asc();
      case KOR_CATEGORY ->
          sortOrder == SortOrder.DESC ? alcohol.korCategory.desc() : alcohol.korCategory.asc();
      case ENG_CATEGORY ->
          sortOrder == SortOrder.DESC ? alcohol.engCategory.desc() : alcohol.engCategory.asc();
      case CREATED_AT ->
          sortOrder == SortOrder.DESC ? alcohol.createAt.desc() : alcohol.createAt.asc();
      case UPDATED_AT ->
          sortOrder == SortOrder.DESC ? alcohol.lastModifyAt.desc() : alcohol.lastModifyAt.asc();
    };
  }

  /** 랜덤 정렬 조건 생성 (시드 없음) */
  public OrderSpecifier<?> sortByRandom() {
    return Expressions.numberTemplate(Double.class, "function('rand')").asc();
  }

  /**
   * 시드 기반 랜덤 정렬 조건 생성. MySQL {@code RAND(seed)} 에 바인딩되며 동일 seed 로 반복 호출 시 동일 순서가 생성된다. explore
   * standard API 의 페이지 간 일관성 보장에 사용된다.
   */
  public OrderSpecifier<?> sortByRandom(long seed) {
    return Expressions.numberTemplate(Double.class, "function('rand', {0})", seed).asc();
  }

  /** HMAC 커서 RANDOM용 CRC32 랭크. {@code CRC32(CONCAT(seed, '-', id))} */
  public NumberExpression<Long> crc32Rank(long seed) {
    return Expressions.numberTemplate(
        Long.class,
        "cast(function('crc32', concat({0}, '-', {1})) as long)",
        String.valueOf(seed),
        alcohol.id);
  }

  /** 삭제되지 않은 데이터 필터 조건 */
  public BooleanExpression isNotDeleted() {
    return alcohol.deletedAt.isNull();
  }

  /** 이름 포함 여부 조건 생성 */
  public BooleanExpression eqName(String name) {
    if (StringUtils.isNullOrEmpty(name)) return null;

    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 카테고리 일치 여부 조건 생성 */
  public BooleanExpression eqCategory(AlcoholCategoryGroup category) {
    if (Objects.isNull(category)) return null;

    return alcohol.categoryGroup.stringValue().like("%" + category + "%");
  }

  /** 지역 일치 여부 조건 생성 (부모 지역이면 하위 지역 포함) */
  public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;
    List<Long> childIds = regionRepository.findChildRegionIds(regionId);
    if (childIds.isEmpty()) return alcohol.region.id.eq(regionId);
    List<Long> regionIds = new java.util.ArrayList<>(childIds.size() + 1);
    regionIds.add(regionId);
    regionIds.addAll(childIds);
    return alcohol.region.id.in(regionIds);
  }

  /**
   * 지역 ID 복수 OR 조건 생성 (각 부모 지역의 하위 지역까지 합집합으로 포함). null/empty 입력 시 조건 생략.
   *
   * <p>성능: 자식 지역은 단일 쿼리 `findChildRegionIdsIn`으로 한 번에 조회하여 N+1을 방지한다.
   */
  public BooleanExpression inRegionIds(List<Long> regionIds) {
    if (regionIds == null || regionIds.isEmpty()) return null;
    List<Long> childIds = regionRepository.findChildRegionIdsIn(regionIds);
    if (childIds.isEmpty()) return alcohol.region.id.in(regionIds);
    java.util.Set<Long> merged = new java.util.LinkedHashSet<>(regionIds.size() + childIds.size());
    merged.addAll(regionIds);
    merged.addAll(childIds);
    return alcohol.region.id.in(merged);
  }

  /** 증류소 ID 복수 OR 조건 생성 (단순 IN, 계층 확장 없음). null/empty 입력 시 조건 생략. */
  public BooleanExpression inDistilleryIds(List<Long> distilleryIds) {
    if (distilleryIds == null || distilleryIds.isEmpty()) return null;
    return alcohol.distillery.id.in(distilleryIds);
  }

  /** 검색 토큰 간 AND, 검색 필드 간 OR 조건 생성 */
  public BooleanExpression searchTokensMatch(List<String> searchTokens) {
    if (searchTokens == null || searchTokens.isEmpty()) {
      return null;
    }

    BooleanExpression finalCondition = null;

    for (String searchToken : searchTokens) {
      if (StringUtils.isNullOrEmpty(searchToken)) {
        continue;
      }
      String likePattern = contains(searchToken);
      BooleanExpression koreanNameCondition = alcohol.korName.likeIgnoreCase(likePattern, ESCAPE);
      if (KOREAN_NAME_TOKEN.matcher(searchToken).matches()) {
        koreanNameCondition =
            koreanNameCondition.or(
                Expressions.stringTemplate("replace({0}, ' ', '')", alcohol.korName)
                    .likeIgnoreCase(likePattern, ESCAPE));
      }
      BooleanExpression basicCondition =
          koreanNameCondition
              .or(alcohol.engName.likeIgnoreCase(likePattern, ESCAPE))
              .or(alcohol.korCategory.likeIgnoreCase(likePattern, ESCAPE))
              .or(alcohol.engCategory.likeIgnoreCase(likePattern, ESCAPE))
              .or(region.korName.likeIgnoreCase(likePattern, ESCAPE))
              .or(region.engName.likeIgnoreCase(likePattern, ESCAPE));

      BooleanExpression tastingTagCondition =
          alcohol.id.in(
              JPAExpressions.selectDistinct(alcoholsTastingTags.alcohol.id)
                  .from(alcoholsTastingTags)
                  .join(tastingTag)
                  .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
                  .where(
                      tastingTag
                          .korName
                          .likeIgnoreCase(likePattern, ESCAPE)
                          .or(tastingTag.engName.likeIgnoreCase(likePattern, ESCAPE))));

      BooleanExpression keywordCondition = basicCondition.or(tastingTagCondition);
      if (finalCondition == null) {
        finalCondition = keywordCondition;
      } else {
        finalCondition = finalCondition.and(keywordCondition);
      }
    }

    return finalCondition;
  }

  /**
   * 키워드 검색 조건 생성
   *
   * @param keyword 검색 키워드
   * @return 검색 조건
   */
  public BooleanExpression keywordMatch(String keyword) {
    if (StringUtils.isNullOrEmpty(keyword)) {
      return null;
    }

    List<String> tokens = AlcoholSearchTokenizer.tokenize(keyword);
    if (tokens.isEmpty()) {
      return null;
    }
    if (tokens.size() > 1) {
      return multiWordSearch(tokens.toArray(String[]::new));
    }
    return singleWordSearch(tokens.getFirst());
  }

  /**
   * 큐레이션 ID로 알코올 검색 조건 생성
   *
   * @param curationId 큐레이션 ID
   * @return 검색 조건
   */
  public BooleanExpression eqCurationId(Long curationId) {
    if (curationId == null) {
      return null;
    }

    return JPAExpressions.selectOne()
        .from(curationKeyword)
        .where(curationKeyword.id.eq(curationId), curationKeyword.alcoholIds.contains(alcohol.id))
        .exists();
  }

  /** 여러 단어 검색 - 각 단어가 모두 포함되어야 함 (순서 무관) */
  private BooleanExpression multiWordSearch(String[] words) {
    BooleanExpression condition = null;

    for (String word : words) {
      if (StringUtils.isNullOrEmpty(word)) continue;

      // 각 단어에 대한 검색 조건 (OR)
      BooleanExpression wordCondition =
          alcohol
              .korName
              .like("%" + word + "%")
              .or(alcohol.engName.like("%" + word + "%"))
              .or(alcohol.korCategory.like("%" + word + "%"))
              .or(alcohol.engCategory.like("%" + word + "%"));

      // 동적 태그 검색 조건 추가
      BooleanExpression tagSearch =
          JPAExpressions.selectOne()
              .from(alcoholsTastingTags)
              .join(tastingTag)
              .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
              .where(
                  alcoholsTastingTags.alcohol.id.eq(alcohol.id),
                  tastingTag
                      .korName
                      .like("%" + word + "%")
                      .or(tastingTag.engName.like("%" + word + "%")))
              .exists();

      wordCondition = wordCondition.or(tagSearch);

      // 각 단어 조건을 AND로 결합
      condition = (condition == null) ? wordCondition : condition.and(wordCondition);
    }

    return condition;
  }

  /** 단일 단어 검색 */
  private BooleanExpression singleWordSearch(String keyword) {
    String keywordWithoutSpaces = keyword.replaceAll("\\s+", "");

    // 띄어쓰기 제거한 키워드로 각 문자 사이에 공백이 0개 이상 올 수 있는 패턴 생성
    String flexiblePattern = buildFlexiblePattern(keywordWithoutSpaces);

    // 기본 검색 조건 (이름, 카테고리)
    BooleanExpression basicSearch =
        alcohol
            .korName
            .like("%" + keyword + "%")
            .or(alcohol.engName.like("%" + keyword + "%"))
            .or(alcohol.korCategory.like("%" + keyword + "%"))
            .or(alcohol.engCategory.like("%" + keyword + "%"));

    // 띄어쓰기 무시 검색 조건 추가
    BooleanExpression flexibleSearch =
        alcohol
            .korName
            .likeIgnoreCase("%" + flexiblePattern + "%")
            .or(alcohol.engName.likeIgnoreCase("%" + flexiblePattern + "%"))
            .or(alcohol.korCategory.likeIgnoreCase("%" + flexiblePattern + "%"))
            .or(alcohol.engCategory.likeIgnoreCase("%" + flexiblePattern + "%"));

    // 동적 태그 검색 조건 추가
    BooleanExpression dynamicTagSearch =
        JPAExpressions.selectOne()
            .from(alcoholsTastingTags)
            .join(tastingTag)
            .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
            .where(
                alcoholsTastingTags.alcohol.id.eq(alcohol.id),
                tastingTag
                    .korName
                    .like("%" + keyword + "%")
                    .or(tastingTag.engName.like("%" + keyword + "%"))
                    .or(tastingTag.korName.likeIgnoreCase("%" + flexiblePattern + "%"))
                    .or(tastingTag.engName.likeIgnoreCase("%" + flexiblePattern + "%")))
            .exists();

    // 모든 조건을 OR로 결합
    return basicSearch.or(flexibleSearch).or(dynamicTagSearch);
  }

  /** 띄어쓰기를 무시한 유연한 패턴 생성 (각 문자 사이에 공백 허용) */
  private String buildFlexiblePattern(String keyword) {
    if (keyword.isEmpty()) return keyword;

    StringBuilder pattern = new StringBuilder();
    for (int i = 0; i < keyword.length(); i++) {
      pattern.append(keyword.charAt(i));
      if (i < keyword.length() - 1) {
        pattern.append("%");
      }
    }
    return pattern.toString();
  }
}
