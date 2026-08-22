package app.bottlenote.history.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.history.controller.docs.UserHistoryApiDocs;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.request.ViewHistoryRequest;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.history.dto.response.ViewHistoryListResponse;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.history.service.UserHistoryQueryService;
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
@UserHistoryApiDocs.ApiTag
public class UserHistoryController {

  private final UserHistoryQueryService userHistoryQueryService;
  private final AlcoholViewHistoryService alcoholViewHistoryService;

  @UserHistoryApiDocs.GetUserHistoryList
  @GetMapping("/{targetUserId}")
  public ResponseEntity<GlobalResponse> findUserHistoryList(
      @PathVariable Long targetUserId,
      @ModelAttribute @Valid UserHistorySearchRequest userHistorySearchRequest) {

    KeysetPageResponse<UserHistorySearchResponse> userHistoryList =
        userHistoryQueryService.findUserHistoryList(targetUserId, userHistorySearchRequest);
    return GlobalResponse.ok(
        userHistoryList.content(),
        MetaService.createMetaInfo().add("pagination", userHistoryList.pagination()));
  }

  @UserHistoryApiDocs.GetViewHistory
  @GetMapping("/view/alcohols")
  public ResponseEntity<GlobalResponse> getViewHistory(
      @ModelAttribute @Valid ViewHistoryRequest request) {
    Long id = getUserIdByContext().orElse(-1L);
    KeysetPageResponse<ViewHistoryListResponse> page =
        alcoholViewHistoryService.getViewHistory(id, request);
    return GlobalResponse.ok(
        page.content(), MetaService.createMetaInfo().add("pagination", page.pagination()));
  }
}
