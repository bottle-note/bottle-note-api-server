package app.bottlenote.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.customMockUser.WithMockCustomUser;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("[Integration] JPA Auditing 통합 테스트")
class JpaAuditingIntegrationTest extends IntegrationTestSupport {

	private ReviewCreateRequest reviewCreateRequest;
	private Long userId;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		reviewCreateRequest = ReviewObjectFixture.getReviewCreateRequest();
		userId = 1L;
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("DB 저장 시 생성자와 수정자가 기록된다.")
	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
		"/init-script/init-user.sql",
		"/init-script/init-review.sql",
		"/init-script/init-review-reply.sql"}
	)
	@Test
	@WithMockCustomUser(username = "user1")
	void test_1() throws Exception {
		log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		MvcResult result = mockMvc.perform(post("/api/v1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewCreateRequest))
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		Review review = mapper.convertValue(response.getData(), mapper.getTypeFactory().constructType(Review.class));

		Review savedReview = reviewRepository.findById(review.getId()).orElseGet(null);

		assertEquals("user1", savedReview.getCreateBy());
	}

}
