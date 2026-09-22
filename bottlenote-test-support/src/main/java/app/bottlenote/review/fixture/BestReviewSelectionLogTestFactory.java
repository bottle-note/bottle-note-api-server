package app.bottlenote.review.fixture;

import app.bottlenote.review.constant.BestReviewChangeType;
import app.bottlenote.review.domain.BestReviewSelectionLog;
import app.bottlenote.review.domain.Review;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class BestReviewSelectionLogTestFactory {

  @Autowired private EntityManager em;

  /** 선정 로그 생성. 수치는 고정값이고 사유는 변동 종류에 맞춰 채운다. */
  @Transactional
  @NotNull
  public BestReviewSelectionLog persistLog(
      @NotNull Review review,
      @NotNull LocalDate selectionDate,
      @NotNull BestReviewChangeType changeType) {
    return persistLog(review, selectionDate, changeType, 1, new BigDecimal("3.20"));
  }

  @Transactional
  @NotNull
  public BestReviewSelectionLog persistLog(
      @NotNull Review review,
      @NotNull LocalDate selectionDate,
      @NotNull BestReviewChangeType changeType,
      Integer ranking,
      @NotNull BigDecimal score) {
    BestReviewSelectionLog log =
        BestReviewSelectionLog.builder()
            .selectionDate(selectionDate)
            .reviewId(review.getId())
            .alcoholId(review.getAlcoholId())
            .changeType(changeType)
            .ranking(ranking)
            .score(score)
            .likeCount(5L)
            .dislikeCount(0L)
            .replyCount(1L)
            .imageCount(2L)
            .contentLength(120)
            .alcoholReviewCount(14L)
            .ruleCode("REVIEW_COUNT_10_TO_19_TOP_2")
            .reason(
                changeType == BestReviewChangeType.SELECTED
                    ? "주류 리뷰 14건 중 1위로 상위 2건 안에 들어 선정되었습니다."
                    : "주류 리뷰 14건 중 3위로 상위 2건 밖으로 밀려 해제되었습니다.")
            .build();
    em.persist(log);
    em.flush();
    em.refresh(log);
    return log;
  }
}
