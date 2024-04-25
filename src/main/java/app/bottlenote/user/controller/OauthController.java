package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.service.OauthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/v1/oauth")
public class OauthController {

	private final OauthService oauthService;

	@GetMapping("/login")
	public ResponseEntity<GlobalResponse> oauthLogin(@RequestBody @Valid OauthRequest oauthReq) {
		log.info("Received oauth request: {}", oauthReq); // Log the request for debugging
		return ResponseEntity.ok(
			GlobalResponse.success(oauthService.oauthLogin(oauthReq)));
	}


}
