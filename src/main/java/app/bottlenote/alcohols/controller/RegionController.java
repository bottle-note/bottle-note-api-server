package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

	private final AlcoholReferenceService alcoholReferenceService;

	@GetMapping
	public ResponseEntity<?> findAll() {
		return GlobalResponse.ok(alcoholReferenceService.findAllRegion());
	}
}
