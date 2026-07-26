package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;
import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.alcohols.controller.docs.AlcoholPopularApiDocs;
import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
import app.bottlenote.alcohols.service.AlcoholPopularService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityPolicy(auth = OPTIONAL_AUTH)
@AlcoholPopularApiDocs.ApiTag
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
  @AlcoholPopularApiDocs.GetPopularOfWeek
  @GetMapping("/popular/week")
  public ResponseEntity<GlobalResponse> getPopularOfWeek(
      @RequestParam(defaultValue = "5") Integer top) {
    Long userId = getUserIdByContext().orElse(-1L);
    var populars = alcoholPopularService.getPopularOfWeek(top, userId);
    var response = PopularsOfWeekResponse.of(populars.size(), populars);
    return GlobalResponse.ok(response);
  }

  /** 봄 추천 인기 위스키 리스트 조회 */
  @AlcoholPopularApiDocs.GetSpringItems
  @GetMapping("/popular/spring")
  public ResponseEntity<GlobalResponse> getSpringItems() {
    Long userId = getUserIdByContext().orElse(-1L);
    var response = alcoholPopularService.getSpringItems(userId);
    return GlobalResponse.ok(response);
  }

  /** 주간 조회수 기반 인기 위스키 리스트 조회 */
  @AlcoholPopularApiDocs.GetPopularByViewsWeekly
  @GetMapping("/popular/view/week")
  public ResponseEntity<GlobalResponse> getPopularByViewsWeekly(
      @RequestParam(defaultValue = "20") Integer top) {
    Long userId = getUserIdByContext().orElse(-1L);
    var populars = alcoholPopularService.getPopularByViewsWeekly(top, userId);
    var response = PopularsOfWeekResponse.of(populars.size(), populars);
    return GlobalResponse.ok(response);
  }

  /** 월간 조회수 기반 인기 위스키 리스트 조회 */
  @AlcoholPopularApiDocs.GetPopularByViewsMonthly
  @GetMapping("/popular/view/monthly")
  public ResponseEntity<GlobalResponse> getPopularByViewsMonthly(
      @RequestParam(defaultValue = "20") Integer top) {
    Long userId = getUserIdByContext().orElse(-1L);
    var populars = alcoholPopularService.getPopularByViewsMonthly(top, userId);
    var response = PopularsOfWeekResponse.of(populars.size(), populars);
    return GlobalResponse.ok(response);
  }
}
