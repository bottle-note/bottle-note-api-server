package app.bottlenote.agreement.controller;

import static app.bottlenote.common.support.HttpClient.getClientIP;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.agreement.controller.docs.AgreementApiDocs;
import app.bottlenote.agreement.dto.request.AgreementSubmitRequest;
import app.bottlenote.agreement.dto.response.AgreementStatusResponse;
import app.bottlenote.agreement.service.AgreementService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.exception.UserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityPolicy(auth = REQUIRED_AUTH)
@RequestMapping("/api/v2/agreements")
@AgreementApiDocs.ApiTag
public class AgreementController {

  private final AgreementService agreementService;

  @GetMapping("/status")
  @AgreementApiDocs.GetStatus
  public ResponseEntity<GlobalResponse> getStatus() {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(AgreementStatusResponse.from(agreementService.getStatus(userId)));
  }

  @PostMapping
  @AgreementApiDocs.Submit
  public ResponseEntity<GlobalResponse> processAgreementSubmission(
      @RequestBody @Valid AgreementSubmitRequest request, HttpServletRequest servletRequest) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    var evaluation =
        agreementService.submit(
            userId,
            request.toSubmissions(),
            getClientIP(servletRequest),
            servletRequest.getHeader(HttpHeaders.USER_AGENT));
    return GlobalResponse.ok(AgreementStatusResponse.from(evaluation));
  }
}
