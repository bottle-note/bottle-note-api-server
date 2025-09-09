package app.bottlenote.common.file.controller;

import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.shared.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/s3")
public class ImageUploadController {

  private final ImageUploadService imageUploadService;

  @GetMapping("/presign-url")
  public ResponseEntity<?> getPreSignUrl(@ModelAttribute ImageUploadRequest request) {
    return ResponseEntity.ok(GlobalResponse.success(imageUploadService.getPreSignUrl(request)));
  }
}
