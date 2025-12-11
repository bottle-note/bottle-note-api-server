package app.bottlenote.support.business.controller;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
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
@RequiredArgsConstructor
public class BusinessSupportController {

  private final BusinessSupportService service;

  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody BusinessSupportUpsertRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.register(req, userId));
  }

  @GetMapping
  public ResponseEntity<?> getAllList(@ModelAttribute BusinessSupportPageableRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    CollectionResponse<BusinessInfoResponse> collection = service.getList(req, userId);
    return GlobalResponse.ok(collection);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getDetail(@PathVariable Long id) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    BusinessSupportDetailItem item = service.getDetail(id, userId);
    return GlobalResponse.ok(item);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> modify(
      @PathVariable Long id, @Valid @RequestBody BusinessSupportUpsertRequest req) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.modify(id, req, userId));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(service.delete(id, userId));
  }
}
