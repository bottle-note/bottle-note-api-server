package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
import app.bottlenote.alcohols.service.PopularService;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

@RestController
@RequestMapping("/api/v1/popular")
@RequiredArgsConstructor
public class PopularQueryController {

	private final PopularService popularService;

	/**
	 * 주간 인기 위스키 리스트 조회
	 * <p>
	 * 유저 아이디가 존재하지않을때 userId를 -1L 로 조회 :
	 * "isPicked": false 값으로만 조회됩니다.
	 *
	 * @param top 조회할 위스키 목록 개수
	 * @return 조회된 위스키 목록
	 */
	@GetMapping("/week")
	public ResponseEntity<?> getPopularOfWeek(@RequestParam(defaultValue = "5") Integer top) {

		Long userId = getUserIdByContext().orElse(-1L);
		List<Populars> populars = popularService.getPopularOfWeek(top, userId);

		return GlobalResponse.ok(PopularsOfWeekResponse.of(populars.size(), populars));
	}
}
