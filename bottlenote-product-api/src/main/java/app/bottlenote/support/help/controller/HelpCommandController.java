package app.bottlenote.support.help.controller;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.support.help.controller.docs.HelpApiDocs;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.support.help.service.HelpService;
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
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
@HelpApiDocs.ApiTag
public class HelpCommandController {

  private final HelpService helpService;

  @HelpApiDocs.RegisterHelp
  @PostMapping
  public ResponseEntity<GlobalResponse> registerHelp(
      @Valid @RequestBody HelpUpsertRequest helpUpsertRequest) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(helpService.registerHelp(helpUpsertRequest, currentUserId));
  }

  @HelpApiDocs.GetHelpList
  @GetMapping
  public ResponseEntity<GlobalResponse> getHelpList(
      @ModelAttribute HelpPageableRequest helpPageableRequest) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new HelpException(REQUIRED_USER_ID));

    KeysetPageResponse<HelpListResponse> pageResponse =
        helpService.getHelpList(helpPageableRequest, currentUserId);

    return GlobalResponse.ok(
        pageResponse.content(),
        MetaService.createMetaInfo().add("pagination", pageResponse.pagination()));
  }

  @HelpApiDocs.GetHelpDetail
  @GetMapping("/{helpId}")
  public ResponseEntity<GlobalResponse> getDetailHelp(@PathVariable Long helpId) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new HelpException(REQUIRED_USER_ID));

    return GlobalResponse.ok(helpService.getDetailHelp(helpId, currentUserId));
  }

  @HelpApiDocs.ModifyHelp
  @PatchMapping("/{helpId}")
  public ResponseEntity<GlobalResponse> modifyHelp(
      @Valid @RequestBody HelpUpsertRequest helpUpsertRequest, @PathVariable Long helpId) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(helpService.modifyHelp(helpUpsertRequest, currentUserId, helpId));
  }

  @HelpApiDocs.DeleteHelp
  @DeleteMapping("/{helpId}")
  public ResponseEntity<GlobalResponse> deleteHelp(@PathVariable Long helpId) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(helpService.deleteHelp(helpId, currentUserId));
  }
}
