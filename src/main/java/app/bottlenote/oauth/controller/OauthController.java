package app.bottlenote.oauth.controller;

import app.bottlenote.oauth.dto.OauthLoginRequest;
import app.bottlenote.oauth.service.OauthLoginService;
import app.bottlenote.security.JwtDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OauthController {

	private final OauthLoginService oauthLoginService;


	@GetMapping("/login")
    public ResponseEntity<JwtDto> OauthLogin(@RequestBody OauthLoginRequest OauthLoginReq){
		return ResponseEntity.ok(oauthLoginService.oauthLogin(OauthLoginReq));
	}

}
