package app.bottlenote.campaigncontent.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.campaigncontent.controller.docs.CampaignContentApiDocs;
import app.bottlenote.campaigncontent.dto.request.CampaignContentEventContextRequest;
import app.bottlenote.campaigncontent.dto.request.CampaignContentEventRequest;
import app.bottlenote.campaigncontent.service.CampaignContentEventService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.observability.visitor.VisitorTelemetryFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaign-contents")
@RequiredArgsConstructor
@SecurityPolicy(auth = OPTIONAL_AUTH)
@CampaignContentApiDocs.ApiTag
public class CampaignContentEventController {

  private final CampaignContentEventService campaignContentEventService;

  @CampaignContentApiDocs.RegisterParticipation
  @PostMapping("/{code}/events")
  public ResponseEntity<GlobalResponse> registerEvent(
      @PathVariable String code,
      @RequestBody @Valid CampaignContentEventRequest request,
      HttpServletRequest servletRequest) {
    // 방문자 식별값·IP·기기 유형은 텔레메트리와 같은 값이 되도록 방문자 필터가 실어 준 속성만 쓴다.
    CampaignContentEventContextRequest context =
        new CampaignContentEventContextRequest(
            SecurityContextUtil.getUserIdByContext().orElse(null),
            getStringAttribute(servletRequest, VisitorTelemetryFilter.VISITOR_ID_ATTRIBUTE),
            getStringAttribute(servletRequest, VisitorTelemetryFilter.CLIENT_IP_ATTRIBUTE),
            getStringAttribute(servletRequest, VisitorTelemetryFilter.DEVICE_TYPE_ATTRIBUTE));
    return GlobalResponse.ok(
        campaignContentEventService.registerEvent(code, request.type(), context));
  }

  private static String getStringAttribute(HttpServletRequest request, String name) {
    return request.getAttribute(name) instanceof String value ? value : null;
  }
}
