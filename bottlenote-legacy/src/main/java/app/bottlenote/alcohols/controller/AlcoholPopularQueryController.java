package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
import app.bottlenote.alcohols.service.AlcoholPopularService;
import app.bottlenote.shared.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlcoholPopularQueryController {

  private final AlcoholPopularService alcoholPopularService;

  /**
   * 주간 인기 위스키 리스트 조회
   *
   * <p>유저 아이디가 존재하지않을때 userId를 -1L 로 조회 : "isPicked": false 값으로만 조회됩니다.
   *
   * @param top 조회할 위스키 목록 개수
   * @return 조회된 위스키 목록
   */
  @GetMapping("/popular/week")
  public ResponseEntity<?> getPopularOfWeek(@RequestParam(defaultValue = "5") Integer top) {
    Long userId = getUserIdByContext().orElse(-1L);
    var populars = alcoholPopularService.getPopularOfWeek(top, userId);
    var response = PopularsOfWeekResponse.of(populars.size(), populars);
    return GlobalResponse.ok(response);
  }

  /** 봄 추천 인기 위스키 리스트 조회 */
  @GetMapping("/popular/spring")
  public ResponseEntity<?> getSpringItems() {
    Long userId = getUserIdByContext().orElse(-1L);
    var response = alcoholPopularService.getSpringItems(userId);
    return GlobalResponse.ok(response);
  }
}
