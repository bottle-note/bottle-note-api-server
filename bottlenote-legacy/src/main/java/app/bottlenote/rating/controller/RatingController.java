package app.bottlenote.rating.controller;

import static app.bottlenote.rating.exception.RatingExceptionCode.REQUEST_USER_ID;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.shared.meta.MetaService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/rating")
public class RatingController {

  private final RatingCommandService commandService;
  private final RatingQueryService queryService;

  public RatingController(RatingCommandService commandService, RatingQueryService queryService) {
    this.commandService = commandService;
    this.queryService = queryService;
  }

  /**
   * 별점을 등록하는 API 입니다.
   *
   * @param request the request
   * @return the response entity
   */
  @PostMapping("/register")
  public ResponseEntity<?> registerRatingPoint(@RequestBody @Valid RatingRegisterRequest request) {
    log.info("RatingRegisterRequest: {}", request);

    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));
    RatingPoint ratingPoint = RatingPoint.of(request.rating());

    return ResponseEntity.ok(
        GlobalResponse.success(commandService.register(request.alcoholId(), userId, ratingPoint)));
  }

  /**
   * 위스키의 별점을 평가하기위해 목록을 조회하는 API 입니다.
   *
   * <p>해당 리스트는 유저가 별점을 평가하지않은 술 목록만 조회됩니다.
   *
   * @param request 별점 평가 목록 조회 요청 객체
   */
  @GetMapping
  public ResponseEntity<?> fetchRatingList(@ModelAttribute RatingListFetchRequest request) {
    Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
    var response = queryService.fetchRatingList(request, userId);
    return ResponseEntity.ok(
        GlobalResponse.success(
            response.content(),
            MetaService.createMetaInfo()
                .add("searchParameters", request)
                .add("pageable", response.cursorPageable())));
  }

  /**
   * 특정한 유저가 어떤 술에 대해 평가한 별점을 조회하는 API 입니다.
   *
   * <p>만약 별점을 평가하지 않은 경우에는 빈 응답을
   *
   * <p>유저 정보는 필수 값입니다
   *
   * @param alcoholId 술의 식별자
   */
  @GetMapping("/{alcoholId}")
  public ResponseEntity<?> fetchRatingPoint(@PathVariable Long alcoholId) {

    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new RatingException(REQUEST_USER_ID));

    UserRatingResponse response = queryService.fetchUserRating(alcoholId, userId);

    return ResponseEntity.ok(GlobalResponse.success(response));
  }
}
