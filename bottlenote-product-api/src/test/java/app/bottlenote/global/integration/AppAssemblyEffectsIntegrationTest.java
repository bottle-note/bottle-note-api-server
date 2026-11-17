package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.common.constant.AuditPrincipalType;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.security.CustomUserContext;
import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.support.block.service.BlockService;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("integration")
class AppAssemblyEffectsIntegrationTest extends IntegrationTestSupport {
  @Autowired private UserTestFactory users;
  @Autowired private AlcoholTestFactory alcohols;
  @Autowired private ReviewTestFactory reviews;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private BlockService blocks;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired private JwtAuthenticationManager jwtAuthenticationManager;
  @Autowired private AuditorAware<AuditPrincipal> auditorAware;

  @Autowired private org.springframework.context.ApplicationContext applicationContext;

  @Test
  @DisplayName("Product 앱에 병합 handler 여섯 종류를 빠짐없이 조립한다")
  void assemblesEveryMergeHandlerType() {
    assertThat(
            applicationContext
                .getBeansOfType(app.bottlenote.alcohols.domain.AlcoholMergeHandler.class)
                .values()
                .stream()
                .map(org.springframework.aop.support.AopUtils::getTargetClass)
                .map(Class::getName)
                .toList())
        .containsExactlyInAnyOrder(
            "app.bottlenote.alcohols.repository.JdbcAlcoholMergeHandler",
            "app.bottlenote.history.repository.JdbcHistoryAlcoholMergeHandler",
            "app.bottlenote.mfds.repository.JdbcMfdsAlcoholMergeHandler",
            "app.bottlenote.picks.repository.JdbcPicksAlcoholMergeHandler",
            "app.bottlenote.rating.repository.JdbcRatingAlcoholMergeHandler",
            "app.bottlenote.review.repository.JdbcReviewAlcoholMergeHandler");
  }

  @Test
  @DisplayName("차단한 작성자의 리뷰를 조회할 때 본문과 닉네임만 마스킹한다")
  void blockedReview_masksContentAndNicknameForCurrentUser() throws Exception {
    // given
    var author = users.persistUser();
    var viewer = users.persistUser();
    var review = reviews.persistReview(author, alcohols.persistAlcohol());
    blocks.blockUser(viewer.getId(), author.getId());

    // when
    JsonNode blocked = detail(review.getId(), viewer);
    JsonNode own = detail(review.getId(), author);

    // then
    assertThat(blocked.at("/reviewInfo/reviewContent").asText()).isEqualTo("차단된 사용자의 글입니다");
    assertThat(blocked.at("/reviewInfo/userInfo/nickName").asText()).isEqualTo("차단된 사용자입니다");
    assertThat(blocked.at("/reviewInfo/userInfo/userId").asLong()).isEqualTo(author.getId());
    assertThat(own.at("/reviewInfo/reviewContent").asText()).isEqualTo(review.getContent());
    assertThat(own.at("/reviewInfo/userInfo/nickName").asText()).isEqualTo(author.getNickName());
    assertThat(reviewRepository.findById(review.getId()).orElseThrow().getContent())
        .isEqualTo(review.getContent());
  }

  @Test
  @DisplayName("인증한 사용자가 리뷰를 생성하고 수정할 때 소유자와 감사 주체를 기록한다")
  void authenticatedReview_recordsOwnerAndCreationAndUpdateAuditors() throws Exception {
    // given
    var author = users.persistUser();
    var alcohol = alcohols.persistAlcohol();
    var token = getToken(author).accessToken();
    var template = ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());
    var create =
        new ReviewCreateRequest(
            template.alcoholId(),
            template.status(),
            template.content(),
            template.sizeType(),
            template.price(),
            template.locationInfo(),
            List.of(),
            template.tastingTagList(),
            template.rating(),
            template.tastingProfile());

    // when
    var created =
        mockMvcTester
            .post()
            .uri("/api/v1/reviews")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(create))
            .header("Authorization", "Bearer " + token)
            .with(csrf())
            .exchange();
    created.assertThat().hasStatusOk();
    long id = mapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asLong();
    var before = reviewRepository.findById(id).orElseThrow();
    assertThat(before.getUserId()).isEqualTo(author.getId());
    assertThat(before.getCreatePrincipal().getId()).isEqualTo(author.getId());
    assertThat(before.getCreatePrincipal().getEmail()).isEqualTo(author.getEmail());
    jdbc.update(
        "UPDATE reviews SET last_modify_at = '2000-01-01 00:00:00', last_modify_principal_id = NULL, last_modify_principal_type = NULL, last_modify_principal_email = NULL WHERE id = ?",
        id);
    var modifyTemplate = ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PUBLIC);
    var modify =
        new ReviewModifyRequest(
            "감사 갱신 확인",
            modifyTemplate.status(),
            modifyTemplate.price(),
            List.of(),
            modifyTemplate.sizeType(),
            modifyTemplate.tastingTagList(),
            modifyTemplate.locationInfo(),
            modifyTemplate.rating(),
            modifyTemplate.tastingProfile());
    var updated =
        mockMvcTester
            .patch()
            .uri("/api/v1/reviews/{id}", id)
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(modify))
            .header("Authorization", "Bearer " + token)
            .with(csrf())
            .exchange();

    // then
    updated.assertThat().hasStatusOk();
    var after = reviewRepository.findById(id).orElseThrow();
    assertThat(after.getContent()).isEqualTo(modify.content());
    assertThat(after.getLastModifyAt()).isAfter(java.time.LocalDateTime.of(2000, 1, 1, 0, 0));
    assertThat(after.getCreatePrincipal().getId()).isEqualTo(author.getId());
    assertThat(after.getLastModifyPrincipal().getId()).isEqualTo(author.getId());
    assertThat(after.getLastModifyPrincipal().getEmail()).isEqualTo(author.getEmail());
  }

  @Test
  @DisplayName("익명 인증 컨텍스트로 저장할 때 인증 없는 저장과 달리 익명 감사 주체를 감사 컬럼에 기록한다")
  void anonymousContext_recordsAnonymousAuditorUnlikeUnauthenticatedSave() {
    // given
    var author = users.persistUser();
    var alcohol = alcohols.persistAlcohol();
    SecurityContextHolder.clearContext();
    Optional<AuditPrincipal> unauthenticatedAuditor = auditorAware.getCurrentAuditor();
    Long unauthenticatedReviewId = reviews.persistReview(author, alcohol).getId();
    // 필터가 토큰 없는 요청에 쓰는 익명 인증(CustomUserContext id=-4)을 운영 bean으로 만든다.
    Authentication anonymous = jwtAuthenticationManager.getAnonymousAuthentication();

    // when
    Optional<AuditPrincipal> anonymousAuditor;
    Long anonymousReviewId;
    SecurityContextHolder.getContext().setAuthentication(anonymous);
    try {
      anonymousAuditor = auditorAware.getCurrentAuditor();
      anonymousReviewId = reviews.persistReview(author, alcohol).getId();
    } finally {
      SecurityContextHolder.clearContext();
    }

    // then
    assertThat(anonymous.getPrincipal()).isInstanceOf(CustomUserContext.class);
    assertThat(((CustomUserContext) anonymous.getPrincipal()).getId()).isEqualTo(-4L);
    assertThat(unauthenticatedAuditor).isEmpty();
    assertThat(anonymousAuditor).isPresent();
    assertThat(anonymousAuditor.orElseThrow().getType()).isEqualTo(AuditPrincipalType.ANONYMOUS);
    assertThat(anonymousAuditor.orElseThrow().getId()).isNull();
    assertThat(anonymousAuditor.orElseThrow().getEmail()).isEqualTo("AnonymousUser@email.com");

    Map<String, Object> unauthenticatedAudit = auditColumns(unauthenticatedReviewId);
    assertThat(unauthenticatedAudit.get("create_principal_type")).isNull();
    assertThat(unauthenticatedAudit.get("create_principal_id")).isNull();
    assertThat(unauthenticatedAudit.get("create_principal_email")).isNull();
    assertThat(unauthenticatedAudit.get("last_modify_principal_type")).isNull();

    Map<String, Object> anonymousAudit = auditColumns(anonymousReviewId);
    assertThat(anonymousAudit.get("create_principal_type")).isEqualTo("ANONYMOUS");
    assertThat(anonymousAudit.get("create_principal_id")).isNull();
    assertThat(anonymousAudit.get("create_principal_email")).isEqualTo("AnonymousUser@email.com");
    assertThat(anonymousAudit.get("last_modify_principal_type")).isEqualTo("ANONYMOUS");
    assertThat(anonymousAudit.get("last_modify_principal_id")).isNull();
    assertThat(anonymousAudit.get("last_modify_principal_email"))
        .isEqualTo("AnonymousUser@email.com");
  }

  private Map<String, Object> auditColumns(Long reviewId) {
    return jdbc.queryForMap(
        "SELECT create_principal_id, create_principal_type, create_principal_email,"
            + " last_modify_principal_id, last_modify_principal_type, last_modify_principal_email"
            + " FROM reviews WHERE id = ?",
        reviewId);
  }

  private JsonNode detail(Long reviewId, User viewer) throws Exception {
    var response =
        mockMvcTester
            .get()
            .uri("/api/v1/reviews/detail/{id}", reviewId)
            .header("Authorization", "Bearer " + getToken(viewer).accessToken())
            .exchange();
    response.assertThat().hasStatusOk();
    return mapper.readTree(response.getResponse().getContentAsString()).path("data");
  }
}
