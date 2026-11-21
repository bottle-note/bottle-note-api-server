package app.bottlenote.support.business.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.support.business.controller.docs.BusinessSupportApiDocs;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.service.BusinessSupportService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-support")
@SecurityPolicy(auth = REQUIRED_AUTH)
@RequiredArgsConstructor
@BusinessSupportApiDocs.ApiTag
public class BusinessSupportController {

  private final BusinessSupportService service;

  @BusinessSupportApiDocs.RegisterBusinessSupport
  @PostMapping
  public ResponseEntity<GlobalResponse> register(
      @Valid @RequestBody BusinessSupportUpsertRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.register(req, userId));
  }

  @BusinessSupportApiDocs.GetBusinessSupportList
  @GetMapping
  public ResponseEntity<GlobalResponse> getAllList(
      @ModelAttribute BusinessSupportPageableRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    var page = service.getList(req, userId);
    return GlobalResponse.ok(
        page.content(), MetaService.createMetaInfo().add("pagination", page.pagination()));
  }

  @BusinessSupportApiDocs.GetBusinessSupportDetail
  @GetMapping("/{id}")
  public ResponseEntity<GlobalResponse> getDetail(@PathVariable Long id) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    BusinessSupportDetailItem item = service.getDetail(id, userId);
    return GlobalResponse.ok(item);
  }

  @BusinessSupportApiDocs.ModifyBusinessSupport
  @PatchMapping("/{id}")
  public ResponseEntity<GlobalResponse> modify(
      @PathVariable Long id, @Valid @RequestBody BusinessSupportUpsertRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.modify(id, req, userId));
  }

  @BusinessSupportApiDocs.DeleteBusinessSupport
  @DeleteMapping("/{id}")
  public ResponseEntity<GlobalResponse> delete(@PathVariable Long id) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.delete(id, userId));
  }
}
