package app.bottlenote.image.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

	private final ImageService imageService;

	@PostMapping
	public ResponseEntity<GlobalResponse> saveProfileImages(
		@RequestParam(value = "profile") MultipartFile image) {

		return ResponseEntity.ok(
			GlobalResponse.success(imageService.saveProfileImages(image))
		);
	}
}
