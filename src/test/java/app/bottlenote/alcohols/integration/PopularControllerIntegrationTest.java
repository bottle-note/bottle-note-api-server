package app.bottlenote.alcohols.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] Popular")
class PopularControllerIntegrationTest extends IntegrationTestSupport {


	@DisplayName("주간 인기 위스키를 조회할 수 있습니다.")
	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
		"/init-script/init-user.sql",
		"/init-script/init-popular_alcohol.sql"
	})
	@Test
	void test_1() throws Exception {
		//given
		// when && then
		MvcResult result = mockMvc.perform(get("/api/v1/popular/week")
				.contentType(MediaType.APPLICATION_JSON)
				.param("top", "5")
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.alcohols").isArray())
			.andExpect(jsonPath("$.data.alcohols.length()").value(5))
			.andReturn();

		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		log.info("response : {}", response);
	}
}
