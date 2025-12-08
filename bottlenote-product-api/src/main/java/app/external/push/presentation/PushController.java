package app.external.push.presentation;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.external.push.application.PushHandler;
import app.external.push.application.UserDeviceService;
import app.external.push.data.request.TokenSaveRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external/push")
@RequiredArgsConstructor
public class PushController {
  private final PushHandler pushHandler;
  private final UserDeviceService deviceService;

  @PostMapping("/token")
  public ResponseEntity<?> saveUserToken(@RequestBody @Valid TokenSaveRequest request) {
    Long userId = getUserIdByContext().orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(
        deviceService.saveUserToken(userId, request.deviceToken(), request.platform()));
  }

  @GetMapping
  public ResponseEntity<?> sendPush(@RequestParam("msg") String message) {
    pushHandler.sendPush(Collections.singletonList(6L), message);
    return GlobalResponse.ok("");
  }
}
