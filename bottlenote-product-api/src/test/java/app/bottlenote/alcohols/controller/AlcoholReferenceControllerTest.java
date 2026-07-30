package app.bottlenote.alcohols.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.service.AlcoholReferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("unit")
class AlcoholReferenceControllerTest {

  @Test
  @DisplayName("레거시 큐레이션 v1 경로는 더 이상 노출하지 않는다")
  void doesNotExposeLegacyCurationV1Endpoints() throws Exception {
    AlcoholReferenceService service = org.mockito.Mockito.mock(AlcoholReferenceService.class);
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new AlcoholReferenceController(service)).build();

    mockMvc.perform(get("/api/v1/curations")).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/curations/{curationId}/alcohols", 1L))
        .andExpect(status().isNotFound());
  }
}
