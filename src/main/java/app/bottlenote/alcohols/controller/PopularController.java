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

import static app.bottlenote.global.security.SecurityUtil.getCurrentUserId;

@RestController
@RequestMapping("/api/v1/popular")
@RequiredArgsConstructor
public class PopularController {

    private final PopularService popularService;

    /**
     * 주간 인기 위스키 리스트 조회
     *
     * @param top 조회할 위스키 목록 개수
     * @return 조회된 위스키 목록
     */
    @GetMapping("week")
    public ResponseEntity<GlobalResponse> getPopularOfWeek(@RequestParam(defaultValue = "5") Integer top) {
		Long userId = getCurrentUserId();

		List<Populars> populars = popularService.getPopularOfWeek(top,userId);
        PopularsOfWeekResponse response = PopularsOfWeekResponse.of(populars.size(), populars);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
