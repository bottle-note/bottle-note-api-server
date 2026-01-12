package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.service.TastingTagService;
import app.bottlenote.global.data.response.GlobalResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasting-tags")
@RequiredArgsConstructor
public class TastingTagController {

  private final TastingTagService tastingTagService;

  @GetMapping("/extract")
  public ResponseEntity<?> getExtractedTags(@RequestParam String text) {
    List<String> tags = tastingTagService.extractTagNames(text);
    return GlobalResponse.ok(tags);
  }
}
