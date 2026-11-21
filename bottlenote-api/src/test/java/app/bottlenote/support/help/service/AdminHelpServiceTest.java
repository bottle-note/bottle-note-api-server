package app.bottlenote.support.help.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.fixture.InMemoryHelpRepository;
import app.bottlenote.user.fixture.InMemoryUserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] AdminHelpService page/size 목록")
class AdminHelpServiceTest {

  @Test
  @DisplayName("page와 size로 문의 목록을 자른다")
  void getHelpList_whenPageAndSize_returnsOffsetSlice() {
    InMemoryHelpRepository helpRepository = new InMemoryHelpRepository();
    AdminHelpService service =
        new AdminHelpService(helpRepository, new InMemoryUserRepository(), event -> {});
    for (int i = 0; i < 5; i++) {
      helpRepository.save(Help.create(1L, HelpType.WHISKEY, "제목" + i, "내용" + i));
    }

    var response = service.getHelpList(AdminHelpPageableRequest.builder().page(1).size(2).build());

    @SuppressWarnings("unchecked")
    List<AdminHelpListResponse.AdminHelpInfo> content =
        (List<AdminHelpListResponse.AdminHelpInfo>) response.getData();
    Map<String, Object> meta = response.getMeta();
    assertThat(content).hasSize(2);
    assertThat(meta)
        .containsEntry("page", 1)
        .containsEntry("size", 2)
        .containsEntry("totalElements", 5L);
  }

  @Test
  @DisplayName("유형 필터를 적용한다")
  void getHelpList_whenTypeFilter_returnsMatchingItems() {
    InMemoryHelpRepository helpRepository = new InMemoryHelpRepository();
    AdminHelpService service =
        new AdminHelpService(helpRepository, new InMemoryUserRepository(), event -> {});
    helpRepository.save(Help.create(1L, HelpType.WHISKEY, "위스키", "내용"));
    helpRepository.save(Help.create(1L, HelpType.REVIEW, "리뷰", "내용"));

    var response =
        service.getHelpList(AdminHelpPageableRequest.builder().type(HelpType.WHISKEY).build());

    @SuppressWarnings("unchecked")
    List<AdminHelpListResponse.AdminHelpInfo> content =
        (List<AdminHelpListResponse.AdminHelpInfo>) response.getData();
    assertThat(content).hasSize(1);
    assertThat(content.getFirst().type()).isEqualTo(HelpType.WHISKEY);
  }
}
