package app.bottlenote.review.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("integration")
@DisplayName("[integration] [controller] ReviewReplyController")
class ReviewReplyIntegrationTest extends IntegrationTestSupport {


	@DisplayName("리뷰의 최상위 댓글을 조회할 수 있다.")
	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
		"/init-script/init-user.sql",
		"/init-script/init-review.sql",
		"/init-script/init-review-reply.sql"}
	)
	@Test
	void test_1() throws Exception {
		log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());
		// given
		final Long reviewId = 4L;
		// when && then
		MvcResult result = mockMvc.perform(get("/api/v1/review/reply/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.param("cursor", "0")
				.param("pageSize", "50")
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andReturn();

		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		List<ReviewReplyInfo> list = mapper.convertValue(response.getData(), mapper.getTypeFactory().constructCollectionType(List.class, ReviewReplyInfo.class));
		list.forEach(log::info);
	}
}
