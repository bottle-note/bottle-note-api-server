package app.bottlenote.fixture;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.constant.KeywordTagMapping;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.TastingTag;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class AlcoholTestFactory {

  private final Random random = new SecureRandom();

  @Autowired private EntityManager em;

  /** 기본 Region 생성 (스코틀랜드) */
  @Transactional
  public Region persistRegion() {
    Region region =
        Region.builder()
            .korName("스코틀랜드-" + generateRandomSuffix())
            .engName("Scotland-" + generateRandomSuffix())
            .continent("Europe")
            .build();
    em.persist(region);
    em.flush();
    return region;
  }

  /** 커스텀 Region 생성 */
  @Transactional
  public Region persistRegion(String korName, String engName) {
    Region region =
        Region.builder()
            .korName(korName + "-" + generateRandomSuffix())
            .engName(engName + "-" + generateRandomSuffix())
            .build();
    em.persist(region);
    em.flush();
    return region;
  }

  /** 빌더를 통한 Region 생성 */
  @Transactional
  public Region persistRegion(Region.RegionBuilder builder) {
    Region region = builder.build();
    em.persist(region);
    em.flush();
    return region;
  }

  /** 기본 Distillery 생성 (맥캘란) */
  @Transactional
  public Distillery persistDistillery() {
    Distillery distillery =
        Distillery.builder()
            .korName("맥캘란-" + generateRandomSuffix())
            .engName("Macallan-" + generateRandomSuffix())
            .logoImgPath("https://example.com/macallan-logo.jpg")
            .build();
    em.persist(distillery);
    em.flush();
    return distillery;
  }

  /** 커스텀 Distillery 생성 */
  @Transactional
  public Distillery persistDistillery(String korName, String engName) {
    Distillery distillery =
        Distillery.builder()
            .korName(korName + "-" + generateRandomSuffix())
            .engName(engName + "-" + generateRandomSuffix())
            .build();
    em.persist(distillery);
    em.flush();
    return distillery;
  }

  /** 빌더를 통한 Distillery 생성 */
  @Transactional
  public Distillery persistDistillery(Distillery.DistilleryBuilder builder) {
    Distillery distillery = builder.build();
    em.persist(distillery);
    em.flush();
    return distillery;
  }

  /** 기본 Alcohol 생성 (위스키) - 연관 엔티티 자동 생성 */
  @Transactional
  public Alcohol persistAlcohol() {
    // 연관 엔티티 자동 생성
    Region region = persistRegionInternal();
    Distillery distillery = persistDistilleryInternal();

    Alcohol alcohol =
        Alcohol.builder()
            .korName("명작 위스키-" + generateRandomSuffix())
            .engName("Masterpiece Whisky-" + generateRandomSuffix())
            .abv("40%")
            .type(AlcoholType.WHISKY)
            .korCategory("위스키")
            .engCategory("Whiskey")
            .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
            .region(region)
            .distillery(distillery)
            .cask("American Oak")
            .imageUrl("https://example.com/image.jpg")
            .build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  @Transactional
  public List<Alcohol> persistAlcohols(int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> persistAlcohol(AlcoholType.WHISKY))
        .toList();
  }

  /** 타입별 Alcohol 생성 - 연관 엔티티 자동 생성 */
  @Transactional
  public Alcohol persistAlcohol(AlcoholType type) {
    // 연관 엔티티 자동 생성
    Region region = persistRegionInternal();
    Distillery distillery = persistDistilleryInternal();

    Alcohol alcohol =
        Alcohol.builder()
            .korName(type.getDefaultKorName() + "-" + generateRandomSuffix())
            .engName(type.getDefaultEngName() + "-" + generateRandomSuffix())
            .abv("40%")
            .type(type)
            .korCategory(type.getKorCategory())
            .engCategory(type.getEngCategory())
            .categoryGroup(type.getDefaultCategoryGroup())
            .region(region)
            .distillery(distillery)
            .cask("Oak")
            .imageUrl("https://example.com/" + type.name().toLowerCase() + ".jpg")
            .build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  /** 이름과 타입으로 Alcohol 생성 - 연관 엔티티 자동 생성 */
  @Transactional
  public Alcohol persistAlcohol(String korName, String engName, AlcoholType type) {
    // 연관 엔티티 자동 생성
    Region region = persistRegionInternal();
    Distillery distillery = persistDistilleryInternal();

    Alcohol alcohol =
        Alcohol.builder()
            .korName(korName + "-" + generateRandomSuffix())
            .engName(engName + "-" + generateRandomSuffix())
            .abv("40%")
            .type(type)
            .korCategory(type.getKorCategory())
            .engCategory(type.getEngCategory())
            .categoryGroup(type.getDefaultCategoryGroup())
            .region(region)
            .distillery(distillery)
            .cask("Oak")
            .imageUrl("https://example.com/custom.jpg")
            .build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  /** 정확한 이름으로 Alcohol 생성 (접미사 없음) - 연관 엔티티 자동 생성 */
  @Transactional
  public Alcohol persistAlcoholWithName(String korName, String engName) {
    // 연관 엔티티 자동 생성
    Region region = persistRegionInternal();
    Distillery distillery = persistDistilleryInternal();

    Alcohol alcohol =
        Alcohol.builder()
            .korName(korName)
            .engName(engName)
            .abv("40%")
            .type(AlcoholType.WHISKY)
            .korCategory("위스키")
            .engCategory("Whiskey")
            .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
            .region(region)
            .distillery(distillery)
            .cask("Oak")
            .imageUrl("https://example.com/custom.jpg")
            .build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  /** 연관 엔티티와 함께 Alcohol 생성 */
  @Transactional
  public Alcohol persistAlcohol(AlcoholType type, Region region, Distillery distillery) {
    Alcohol alcohol =
        Alcohol.builder()
            .korName(type.getDefaultKorName() + "-" + generateRandomSuffix())
            .engName(type.getDefaultEngName() + "-" + generateRandomSuffix())
            .abv("40%")
            .type(type)
            .korCategory(type.getKorCategory())
            .engCategory(type.getEngCategory())
            .categoryGroup(type.getDefaultCategoryGroup())
            .region(region)
            .distillery(distillery)
            .cask("Oak")
            .imageUrl("https://example.com/" + type.name().toLowerCase() + ".jpg")
            .build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  /** 빌더를 통한 Alcohol 생성 - 누락 필드 자동 채우기 */
  @Transactional
  public Alcohol persistAlcohol(Alcohol.AlcoholBuilder builder) {
    // 누락 필드 채우기
    Alcohol.AlcoholBuilder filledBuilder = fillMissingAlcoholFields(builder);
    Alcohol alcohol = filledBuilder.build();
    em.persist(alcohol);
    em.flush();
    return alcohol;
  }

  /** 빌더를 통한 Alcohol 생성 후 flush (즉시 ID 필요한 경우) */
  @Transactional
  public Alcohol persistAndFlushAlcohol(Alcohol.AlcoholBuilder builder) {
    // 누락 필드 채우기
    Alcohol.AlcoholBuilder filledBuilder = fillMissingAlcoholFields(builder);
    Alcohol alcohol = filledBuilder.build();
    em.persist(alcohol);
    em.flush(); // 즈시 ID 필요한 경우에만 사용
    return alcohol;
  }

  /** 랜덤 접미사 생성 헬퍼 메서드 */
  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  /** 내부용 Region 생성 (트랜잭션 전파 없음) */
  private Region persistRegionInternal() {
    Region region =
        Region.builder()
            .korName("스코틀랜드-" + generateRandomSuffix())
            .engName("Scotland-" + generateRandomSuffix())
            .continent("Europe")
            .build();
    em.persist(region);
    return region;
  }

  /** 내부용 Distillery 생성 (트랜잭션 전파 없음) */
  private Distillery persistDistilleryInternal() {
    Distillery distillery =
        Distillery.builder()
            .korName("맥캘란-" + generateRandomSuffix())
            .engName("Macallan-" + generateRandomSuffix())
            .logoImgPath("https://example.com/macallan-logo.jpg")
            .build();
    em.persist(distillery);
    return distillery;
  }

  /** Alcohol 빌더의 누락 필드 채우기 */
  private Alcohol.AlcoholBuilder fillMissingAlcoholFields(Alcohol.AlcoholBuilder builder) {
    // 빌더를 임시로 빌드해서 필드 체크
    Alcohol tempAlcohol;
    try {
      tempAlcohol = builder.build();
    } catch (Exception e) {
      // 필수 필드 누락 시 기본값으로 채우기
      AlcoholType defaultType = AlcoholType.WHISKY;
      return builder
          .korName(defaultType.getDefaultKorName() + "-" + generateRandomSuffix())
          .engName(defaultType.getDefaultEngName() + "-" + generateRandomSuffix())
          .abv("40%")
          .type(defaultType)
          .korCategory(defaultType.getKorCategory())
          .engCategory(defaultType.getEngCategory())
          .categoryGroup(defaultType.getDefaultCategoryGroup())
          .region(persistRegionInternal())
          .distillery(persistDistilleryInternal())
          .cask("Oak")
          .imageUrl("https://example.com/default.jpg");
    }

    // 개별 필드 체크 및 채우기
    if (tempAlcohol.getKorName() == null) {
      AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
      builder.korName(type.getDefaultKorName() + "-" + generateRandomSuffix());
    }
    if (tempAlcohol.getEngName() == null) {
      AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
      builder.engName(type.getDefaultEngName() + "-" + generateRandomSuffix());
    }
    if (tempAlcohol.getType() == null) {
      builder.type(AlcoholType.WHISKY);
    }
    if (tempAlcohol.getAbv() == null) {
      builder.abv("40%");
    }
    if (tempAlcohol.getKorCategory() == null) {
      AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
      builder.korCategory(type.getKorCategory());
    }
    if (tempAlcohol.getEngCategory() == null) {
      AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
      builder.engCategory(type.getEngCategory());
    }
    if (tempAlcohol.getCategoryGroup() == null) {
      AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
      builder.categoryGroup(type.getDefaultCategoryGroup());
    }
    if (tempAlcohol.getRegion() == null) {
      builder.region(persistRegionInternal());
    }
    if (tempAlcohol.getDistillery() == null) {
      builder.distillery(persistDistilleryInternal());
    }
    if (tempAlcohol.getCask() == null) {
      builder.cask("Oak");
    }
    if (tempAlcohol.getImageUrl() == null) {
      builder.imageUrl("https://example.com/default.jpg");
    }

    return builder;
  }

  @Transactional
  public Set<AlcoholsTastingTags> appendTastingTag(Alcohol alcohol, TastingTag tag) {
    if (tag == null) {
      tag =
          TastingTag.builder()
              .korName("테스트 태그-" + generateRandomSuffix())
              .engName("test-tag-" + generateRandomSuffix())
              .build();
    }
    if (alcohol == null) {
      alcohol = persistAlcohol();
    }

    em.persist(tag);
    em.flush();

    AlcoholsTastingTags tastingTags =
        AlcoholsTastingTags.builder().alcohol(alcohol).tastingTag(tag).build();

    em.persist(tastingTags);

    // 양방향 연관관계 설정
    alcohol.getAlcoholsTastingTags().add(tastingTags);
    em.flush();

    // 새로운 Set을 반환해서 Lazy 문제 회피
    return new HashSet<>(alcohol.getAlcoholsTastingTags());
  }

  @Transactional
  public Set<AlcoholsTastingTags> getAlcoholTastingTags(Long alcoholId) {
    List<AlcoholsTastingTags> result =
        em.createQuery(
                """
						SELECT att
						FROM alcohol_tasting_tags att
						JOIN FETCH att.tastingTag
						WHERE att.alcohol.id = :alcoholId
						""",
                AlcoholsTastingTags.class)
            .setParameter("alcoholId", alcoholId)
            .getResultList();

    return new HashSet<>(result);
  }

  /**
   * KeywordTagMapping에 따라 알코올에 태그를 설정 기존 태그는 모두 삭제하고 새로 설정
   *
   * @deprecated CurationKeyword 엔티티로 대체되었습니다. 향후 제거될 예정입니다.
   */
  @Deprecated(since = "2025-01", forRemoval = true)
  @Transactional
  public void appendTagsFromKeywordMapping(Long alcoholId, KeywordTagMapping mapping) {
    // 1. 알코올 조회
    Alcohol alcohol = em.find(Alcohol.class, alcoholId);
    if (alcohol == null) {
      throw new IllegalArgumentException("알코올이 존재하지 않습니다. ID: " + alcoholId);
    }

    // 2. 기존 태그 매핑 삭제
    em.createQuery(
            """
						DELETE FROM alcohol_tasting_tags att
						WHERE att.alcohol.id = :alcoholId
						""")
        .setParameter("alcoholId", alcoholId)
        .executeUpdate();

    // 3. includeTags로 태그 생성/조회 후 매핑
    List<Long> includeTagIds = mapping.getIncludeTags();
    for (Long tagId : includeTagIds) {
      // 기존 태그 조회
      TastingTag tag = em.find(TastingTag.class, tagId);
      if (tag == null) {
        // 없으면 특정 ID로 생성
        String korName = "태그-" + tagId;
        String engName = "tag-" + tagId;

        // Native Query로 직접 INSERT (ID 지정)
        em.createNativeQuery(
                """
                                INSERT INTO tasting_tags (id, kor_name, eng_name, create_at, last_modify_at)
                                VALUES (?, ?, ?, NOW(), NOW())
                                """)
            .setParameter(1, tagId)
            .setParameter(2, korName)
            .setParameter(3, engName)
            .executeUpdate();

        // 생성된 엔티티 조회해서 할당
        tag = em.find(TastingTag.class, tagId);
      }

      AlcoholsTastingTags tastingTags =
          AlcoholsTastingTags.builder().alcohol(alcohol).tastingTag(tag).build();
      em.persist(tastingTags);
    }

    em.flush();
  }

  /** 기본 CurationKeyword 생성 */
  @Transactional
  public CurationKeyword persistCurationKeyword() {
    CurationKeyword curation =
        CurationKeyword.builder()
            .name("테스트 큐레이션-" + generateRandomSuffix())
            .description("테스트용 큐레이션 설명")
            .coverImageUrl("https://example.com/curation-cover.jpg")
            .isActive(true)
            .displayOrder(0)
            .alcoholIds(new HashSet<>())
            .build();
    em.persist(curation);
    em.flush();
    return curation;
  }

  /** 알코올 ID 목록과 함께 CurationKeyword 생성 */
  @Transactional
  public CurationKeyword persistCurationKeyword(
      String name, String description, Set<Long> alcoholIds) {
    CurationKeyword curation =
        CurationKeyword.builder()
            .name(name)
            .description(description)
            .coverImageUrl("https://example.com/curation-cover.jpg")
            .isActive(true)
            .displayOrder(0)
            .alcoholIds(alcoholIds != null ? new HashSet<>(alcoholIds) : new HashSet<>())
            .build();
    em.persist(curation);
    em.flush();
    return curation;
  }

  /** 알코올 리스트와 함께 CurationKeyword 생성 */
  @Transactional
  public CurationKeyword persistCurationKeyword(String name, List<Alcohol> alcohols) {
    Set<Long> alcoholIds = new HashSet<>();
    if (alcohols != null) {
      alcohols.forEach(alcohol -> alcoholIds.add(alcohol.getId()));
    }

    CurationKeyword curation =
        CurationKeyword.builder()
            .name(name)
            .description(name + " 설명")
            .coverImageUrl("https://example.com/curation-cover.jpg")
            .isActive(true)
            .displayOrder(0)
            .alcoholIds(alcoholIds)
            .build();
    em.persist(curation);
    em.flush();
    return curation;
  }
}
