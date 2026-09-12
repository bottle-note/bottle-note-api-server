package app.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 스펙 문서의 최상단 정보와 인증 방식을 정의한다. 개별 엔드포인트 설명은 각 도메인의 문서 어노테이션이 담당한다. */
@Configuration
public class OpenApiConfig {

  /**
   * 액세스 토큰을 Authorization 헤더로 전달하는 방식을 가리키는 이름.
   *
   * <p>components 하위 키는 OpenAPI 규칙상 영문 식별자여야 한다. 사람이 읽을 설명은 description에 담는다.
   */
  public static final String BEARER_AUTH = "bearerAuth";

  /** Scalar가 사이드바를 상위 그룹으로 접을 때 읽는 확장 키. */
  public static final String TAG_GROUPS = "x-tagGroups";

  /**
   * 사이드바에 나올 메뉴 전체. 그룹 순서와 그룹 안의 태그 순서가 그대로 화면 순서가 된다.
   *
   * <p>태그의 이름과 설명은 여기에만 적는다. 문서 어노테이션에 설명을 함께 적으면 springdoc이 태그 목록을 다시 정렬해서 이 순서가 깨지고, 같은 이름을 설명만
   * 다르게 선언하면 스펙에 태그가 중복으로 실린다.
   */
  private static final List<TagGroup> MENU =
      List.of(
          group(
              "계정과 인증",
              tag("인증", "소셜 로그인과 토큰 발급·검증을 처리한다"),
              tag("사용자 동의", "인증 사용자의 약관 동의 상태를 조회하고 의사표시를 기록한다"),
              tag("회원 정보", "닉네임과 프로필 이미지를 바꾸고 탈퇴를 처리한다"),
              tag("팔로우", "다른 사용자를 팔로우하고 목록을 조회한다"),
              tag("차단", "다른 사용자를 차단하고 차단 관계를 조회한다")),
          group(
              "위스키",
              tag("위스키 조회", "위스키를 검색하고 상세 정보를 확인한다"),
              tag("위스키 기준 정보", "지역과 종류 같은 선택 목록, 그리고 큐레이션을 조회한다"),
              tag("인기 위스키", "기간과 기준별로 집계한 인기 위스키를 제공한다"),
              tag("테이스팅 태그", "리뷰 문장에서 맛과 향 표현을 뽑아낸다"),
              tag("둘러보기", "원하는 기준으로 콘텐츠를 둘러본다"),
              tag("수입 정보", "식약처 수입 주류와 수입사를 조회한다")),
          group(
              "리뷰",
              tag("리뷰", "위스키에 대한 리뷰를 작성하고 조회하고 수정한다"),
              tag("리뷰 댓글", "리뷰에 달리는 댓글과 대댓글을 관리한다"),
              tag("좋아요", "리뷰에 좋아요를 누르거나 취소한다")),
          group(
              "내 활동",
              tag("마이페이지", "사용자가 남긴 리뷰·별점·찜 기록을 모아 보여준다"),
              tag("찜하기", "관심 있는 위스키를 찜하거나 해제한다"),
              tag("별점", "위스키에 별점을 주고 조회한다"),
              tag("활동 기록", "사용자의 활동 내역과 최근 본 위스키를 조회한다"),
              tag("알림함", "인증 사용자의 알림 목록을 조회하고 읽음 처리한다")),
          group(
              "큐레이션",
              tag("큐레이션", "기획으로 엮은 위스키 모음과 피드를 조회한다"),
              tag("큐레이션 명세", "큐레이션이 어떤 항목으로 구성되는지 정의한 명세를 조회한다")),
          group(
              "고객 지원",
              tag("문의", "서비스 이용 중 생긴 문의를 남기고 답변을 확인한다"),
              tag("신고", "부적절한 사용자나 리뷰를 신고한다"),
              tag("비즈니스 문의", "제휴와 협업 문의를 등록하고 관리한다")),
          group(
              "공통",
              tag("이미지 업로드", "이미지를 직접 올릴 수 있는 임시 주소를 발급한다"),
              tag("배너", "앱 화면에 노출할 배너를 조회한다"),
              tag("서버 정보", "현재 배포된 서버의 버전과 환경 정보를 확인한다")));

  @Bean
  OpenAPI productOpenApi() {
    OpenAPI openApi =
        new OpenAPI().info(productApiInfo()).tags(declaredTags()).components(securityComponents());
    openApi.addExtension(TAG_GROUPS, tagGroups());
    return openApi;
  }

  private Info productApiInfo() {
    return new Info()
        .title("보틀노트 Product API")
        .version("v1")
        .description(
            """
            보틀노트 앱이 사용하는 공개 API입니다.

            모든 응답은 success, code, data, errors, meta를 갖는 공통 형식으로 감싸여 있고, \
            실제 결과값은 data 안에 담깁니다.

            인증이 필요한 엔드포인트는 로그인 후 발급받은 액세스 토큰을 Authorization 헤더에 \
            "Bearer {토큰}" 형태로 담아 호출합니다.""");
  }

  private List<Tag> declaredTags() {
    return MENU.stream().flatMap(group -> group.tags().stream()).toList();
  }

  private List<Map<String, Object>> tagGroups() {
    return MENU.stream().map(TagGroup::toExtensionEntry).toList();
  }

  private Components securityComponents() {
    return new Components()
        .addSecuritySchemes(
            BEARER_AUTH,
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 응답으로 받은 액세스 토큰을 그대로 입력합니다."));
  }

  private static TagGroup group(String name, Tag... tags) {
    return new TagGroup(name, List.of(tags));
  }

  private static Tag tag(String name, String description) {
    return new Tag().name(name).description(description);
  }

  /** 사이드바의 상위 그룹 하나와 그 안에 들어가는 태그들. */
  private record TagGroup(String name, List<Tag> tags) {

    /** 그룹에서 빠진 태그는 Scalar 사이드바에 아예 나오지 않으므로 태그 목록과 같은 출처에서 만든다. */
    Map<String, Object> toExtensionEntry() {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("name", name);
      entry.put("tags", tags.stream().map(Tag::getName).toList());
      return entry;
    }
  }
}
