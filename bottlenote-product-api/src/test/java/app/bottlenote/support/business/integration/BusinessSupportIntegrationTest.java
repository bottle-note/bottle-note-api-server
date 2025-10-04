package app.bottlenote.support.business.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.support.business.constant.BusinessSupportType;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.fixture.BusinessSupportTestFactory;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] BusinessSupportController")
class BusinessSupportIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userFactory;
  @Autowired private BusinessSupportTestFactory businessFactory;
  @Autowired private BusinessSupportRepository repository;

  @Test
  @DisplayName("비지니스 문의를 등록할 수 있다.")
  void register() throws Exception {
    BusinessSupportUpsertRequest req =
        new BusinessSupportUpsertRequest(
            "이벤트 협업 관련 문의드려요", "blah blah", "test@naver.com", BusinessSupportType.EVENT, List.of());

    mockMvc
        .perform(
            post("/api/v1/business-support")
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(req))
                .header("Authorization", "Bearer " + getToken())
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();

    assertEquals(1, repository.findAll().size());
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 문의를 등록할 수 없다.")
  void register_fail_unauthorized() throws Exception {
    // given
    BusinessSupportUpsertRequest req =
        new BusinessSupportUpsertRequest(
            "이벤트 협업 관련 문의드려요", "blah blah", "test@naver.com", BusinessSupportType.EVENT, List.of());

    // when & then
    mockMvc
        .perform(
            post("/api/v1/business-support")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("자신이 등록한 문의 목록을 조회할 수 있다. (200 OK)")
  void get_list_success() throws Exception {
    // given
    User user = userFactory.persistUser();
    businessFactory.persist(user.getId());

    // when & then
    mockMvc
        .perform(get("/api/v1/business-support").header("Authorization", "Bearer " + getToken()))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("자신이 등록한 문의 상세 내용을 조회할 수 있다. (200 OK)")
  void get_detail_success() throws Exception {
    // given
    User user = userFactory.persistUser();
    BusinessSupport support = businessFactory.persist(user.getId());

    // when & then
    mockMvc
        .perform(
            get("/api/v1/business-support/{id}", support.getId())
                .header("Authorization", "Bearer " + getToken()))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("존재하지 않는 문의는 상세 조회할 수 없다. (404 Not Found)")
  void get_detail_fail_not_found() throws Exception {
    // given
    long nonExistId = 999L;

    // when & then
    mockMvc
        .perform(
            get("/api/v1/business-support/{id}", nonExistId)
                .header("Authorization", "Bearer " + getToken()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("자신의 문의를 성공적으로 수정할 수 있다. (200 OK)")
  void modify_success() throws Exception {
    // given
    User user = userFactory.persistUser();
    BusinessSupport support = businessFactory.persist(user.getId());
    BusinessSupportUpsertRequest req =
        new BusinessSupportUpsertRequest(
            "이벤트 협업 관련 문의드려요", "blah blah", "test@naver.com", BusinessSupportType.EVENT, List.of());

    // when & then
    mockMvc
        .perform(
            patch("/api/v1/business-support/{id}", support.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + getToken())
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("자신의 문의를 성공적으로 삭제할 수 있다. (200 OK)")
  void delete_success() throws Exception {
    // given
    User user = userFactory.persistUser();
    BusinessSupport support = businessFactory.persist(user.getId());

    // when & then
    mockMvc
        .perform(
            delete("/api/v1/business-support/{id}", support.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + getToken()))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
