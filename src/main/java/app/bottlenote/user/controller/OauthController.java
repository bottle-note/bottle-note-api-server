package app.bottlenote.user.controller;

import app.bottlenote.common.jwt.dto.request.OauthRequest;
import app.bottlenote.user.service.OauthService;
import app.bottlenote.common.jwt.dto.response.OauthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OauthController {

	private final OauthService oauthLoginService;

	@PostMapping("/login")
    public ResponseEntity<OauthResponse> oauthLogin(@RequestBody OauthRequest oauthReq){
		return ResponseEntity.ok(oauthLoginService.oauthLogin(oauthReq));
	}

}
