package app.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
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

  /**
   * 사이드바에 나올 메뉴 전체. 선언 순서가 그대로 화면 순서가 된다.
   *
   * <p>태그의 이름과 설명은 여기에만 적는다. 문서 어노테이션에 설명을 함께 적으면 springdoc이 태그 목록을 다시 정렬해서 이 순서가 깨지고, 같은 이름을 설명만
   * 다르게 선언하면 스펙에 태그가 중복으로 실린다.
   */
  private static final List<Tag> MENU =
      List.of(
          tag("인증", "소셜 로그인과 토큰 발급·검증, 약관 동의를 처리한다"),
          tag("회원", "닉네임과 프로필을 관리하고 다른 사용자를 팔로우한다"),
          tag("차단", "다른 사용자를 차단하고 차단 관계를 조회한다"),
          tag("위스키", "위스키를 검색·조회하고 인기 순위, 기준 정보, 테이스팅 태그를 제공한다"),
          tag("둘러보기", "원하는 기준으로 콘텐츠를 둘러본다"),
          tag("수입 정보", "식약처 수입 주류와 수입사를 조회한다"),
          tag("큐레이션", "기획으로 엮은 위스키 모음과 그 구성 명세를 조회한다"),
          tag("리뷰", "리뷰와 댓글을 작성·조회하고 좋아요를 누른다"),
          tag("마이페이지", "내가 남긴 리뷰·별점·찜과 활동 기록을 모아 본다"),
          tag("알림함", "인증 사용자의 알림 목록을 조회하고 읽음 처리한다"),
          tag("고객 지원", "문의와 신고, 제휴 문의를 등록하고 답변을 확인한다"),
          tag("공통", "이미지 업로드 주소, 배너, 서버 정보를 제공한다"));

  @Bean
  OpenAPI productOpenApi() {
    return new OpenAPI().info(productApiInfo()).tags(MENU).components(securityComponents());
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

  private static Tag tag(String name, String description) {
    return new Tag().name(name).description(description);
  }
}
