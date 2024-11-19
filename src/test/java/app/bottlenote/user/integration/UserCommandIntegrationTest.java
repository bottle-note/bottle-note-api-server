package app.bottlenote.user.integration;

import static app.bottlenote.user.domain.constant.UserStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.repository.UserCommandRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserCommandController")
@WithMockUser
class UserCommandIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private UserCommandRepository userCommandRepository;

	private OauthRequest oauthRequest;

	@BeforeEach
	void setUp() {
		oauthRequest = new OauthRequest("chadongmin@naver.com", SocialType.KAKAO, null, null);
	}

	@Sql(scripts = {
		"/init-script/init-user.sql"}
	)
	@DisplayName("회원탈퇴에 성공한다.")
	@Test
	void test_1() throws Exception {
		// given


		MvcResult result = mockMvc.perform(delete("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		WithdrawUserResultResponse withdrawUserResultResponse = mapper.convertValue(response.getData(), WithdrawUserResultResponse.class);

		userCommandRepository.findById(withdrawUserResultResponse.userId())
			.ifPresent(withdraw -> assertEquals(DELETED, withdraw.getStatus()));
	}
}
