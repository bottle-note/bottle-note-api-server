package app.bottlenote.notification.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.notification.controller.docs.NotificationPreferenceApiDocs;
import app.bottlenote.notification.dto.request.NotificationPreferenceRequest;
import app.bottlenote.notification.service.NotificationPreferenceService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@SecurityPolicy(auth = REQUIRED_AUTH)
@NotificationPreferenceApiDocs.ApiTag
public class NotificationPreferenceController {
  private final NotificationPreferenceService preferenceService;

  @GetMapping
  @NotificationPreferenceApiDocs.GetPreferences
  public ResponseEntity<GlobalResponse> getPreferences() {
    return GlobalResponse.ok(preferenceService.getPreferences(currentUserId()));
  }

  @PatchMapping
  @NotificationPreferenceApiDocs.UpdatePreferences
  public ResponseEntity<GlobalResponse> updatePreferences(
      @Valid @RequestBody NotificationPreferenceRequest request) {
    return GlobalResponse.ok(preferenceService.updatePreferences(currentUserId(), request));
  }

  private Long currentUserId() {
    return SecurityContextUtil.getUserIdByContext()
        .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
  }
}
