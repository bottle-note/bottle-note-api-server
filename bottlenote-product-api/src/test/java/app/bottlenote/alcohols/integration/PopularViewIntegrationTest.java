package app.bottlenote.alcohols.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] Popular View")
class PopularViewIntegrationTest extends IntegrationTestSupport {

  @DisplayName("주간 조회수 기반 인기 위스키를 조회할 수 있다")
  @Sql(
      scripts = {
        "/init-script/init-alcohol.sql",
        "/init-script/init-user.sql",
        "/init-script/init-alcohols_view_history.sql",
        "/init-script/init-rating.sql"
      })
  @Test
  void test_getPopularViewWeekly() throws Exception {
    // given
    int top = 5;

    // when && then
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/popular/view/week")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("top", String.valueOf(top))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.alcohols").isArray())
            .andExpect(jsonPath("$.data.alcohols.length()").value(top))
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    log.info("response : {}", response);
  }

  @DisplayName("조회 기록이 부족하면 평점 높은 주류로 채워서 반환한다")
  @Sql(
      scripts = {
        "/init-script/init-alcohol.sql",
        "/init-script/init-user.sql",
        "/init-script/init-alcohols_view_history.sql",
        "/init-script/init-rating.sql"
      })
  @Test
  void test_getPopularViewWeekly_fillWithRating() throws Exception {
    // given - 조회 기록 5개, 요청 10개 -> 5개는 평점 기반으로 채워짐
    int top = 10;

    // when && then
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/popular/view/week")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("top", String.valueOf(top))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.alcohols").isArray())
            .andExpect(jsonPath("$.data.totalCount").value(top))
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    log.info("response : {}", response);
  }

  @DisplayName("월간 조회수 기반 인기 위스키를 조회할 수 있다")
  @Sql(
      scripts = {
        "/init-script/init-alcohol.sql",
        "/init-script/init-user.sql",
        "/init-script/init-alcohols_view_history.sql",
        "/init-script/init-rating.sql"
      })
  @Test
  void test_getPopularViewMonthly() throws Exception {
    // given
    int top = 5;

    // when && then
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/popular/view/monthly")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("top", String.valueOf(top))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.alcohols").isArray())
            .andExpect(jsonPath("$.data.alcohols.length()").value(top))
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    log.info("response : {}", response);
  }
}
