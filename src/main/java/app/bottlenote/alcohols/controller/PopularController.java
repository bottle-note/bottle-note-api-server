package app.bottlenote.alcohols.controller;


import app.bottlenote.global.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/popular")
public class PopularController {

	@GetMapping("week")
	public ResponseEntity<GlobalResponse> getPopularsOfWeek(@RequestParam(defaultValue = "5") Integer top) {

		//todo : 주간 인기 술 리스트 조회

		return null;
	}
}
