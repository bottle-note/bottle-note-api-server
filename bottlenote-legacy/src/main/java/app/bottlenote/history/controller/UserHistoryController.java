package app.bottlenote.history.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.history.service.UserHistoryQueryService;
import app.bottlenote.shared.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.data.response.CollectionResponse;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.shared.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class UserHistoryController {

  private final UserHistoryQueryService userHistoryQueryService;
  private final AlcoholViewHistoryService alcoholViewHistoryService;

  @GetMapping("/{targetUserId}")
  public ResponseEntity<?> findUserHistoryList(
      @PathVariable Long targetUserId,
      @ModelAttribute @Valid UserHistorySearchRequest userHistorySearchRequest) {

    PageResponse<UserHistorySearchResponse> userHistoryList =
        userHistoryQueryService.findUserHistoryList(targetUserId, userHistorySearchRequest);
    return GlobalResponse.ok(
        userHistoryList.content(),
        MetaService.createMetaInfo().add("pageable", userHistoryList.cursorPageable()));
  }

  @GetMapping("/view/alcohols")
  public ResponseEntity<?> getViewHistory() {
    Long id = getUserIdByContext().orElse(-1L);
    CollectionResponse<ViewHistoryItem> response = alcoholViewHistoryService.getViewHistory(id);
    return GlobalResponse.ok(response);
  }
}
