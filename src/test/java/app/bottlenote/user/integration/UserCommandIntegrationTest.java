package app.bottlenote.user.integration;

import static app.bottlenote.user.domain.constant.UserStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserObjectFixture;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

@Tag("integration")
@DisplayName("[integration] [controller] UserCommandController")
@WithMockUser
class UserCommandIntegrationTest extends IntegrationTestSupport {

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Autowired
	private UserCommandRepository userCommandRepository;

	@BeforeEach
	void setUp() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Sql(scripts = {
		"/init-script/init-user.sql"}
	)

	@DisplayName("회원탈퇴에 성공한다.")
	@Test
	void test_1() throws Exception {
		// given
		User user = UserObjectFixture.getUserObject();
		log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(1L));

		mockMvc.perform(delete("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		User withdrawUser = userCommandRepository.findById(user.getId())
			.orElse(null);

		assertEquals(DELETED, withdrawUser.getStatus());

	}
}
