package app.bottlenote.picks.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.repository.PicksRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.constant.PicksStatus.UNPICK;
import static app.bottlenote.picks.dto.response.PicksUpdateResponse.Message.PICKED;
import static app.bottlenote.picks.dto.response.PicksUpdateResponse.Message.UNPICKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] PickController")
class PicksIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private PicksRepository picksRepository;

	@DisplayName("찜을 등록할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	void test_1() throws Exception {

		PicksUpdateRequest picksUpdateRequest = new PicksUpdateRequest(1L, PICK);

		MvcResult result = mockMvc.perform(put("/api/v1/picks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(picksUpdateRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		PicksUpdateResponse picksUpdateResponse = mapper.convertValue(response.getData(), PicksUpdateResponse.class);

		assertEquals(picksUpdateResponse.message(), PICKED.message());
	}

	@DisplayName("등록한 찜을 해제할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	void test_2() throws Exception {

		PicksUpdateRequest registerPicksRequest = new PicksUpdateRequest(1L, PICK);
		PicksUpdateRequest unregisterPicksRequest = new PicksUpdateRequest(1L, UNPICK);

		mockMvc.perform(put("/api/v1/picks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(registerPicksRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists());

		Picks picks = picksRepository.findByAlcoholIdAndUserId(1L, getTokenUserId()).orElse(null);
		assertNotNull(picks);
		assertEquals(PICK, picks.getStatus());

		MvcResult result = mockMvc.perform(put("/api/v1/picks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(unregisterPicksRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		PicksUpdateResponse picksUpdateResponse = mapper.convertValue(response.getData(), PicksUpdateResponse.class);

		assertEquals(picksUpdateResponse.message(), UNPICKED.message());
		Picks unPick = picksRepository.findByAlcoholIdAndUserId(1L, getTokenUserId()).orElse(null);
		assertNotNull(unPick);
		assertEquals(UNPICK, unPick.getStatus());

	}
}
