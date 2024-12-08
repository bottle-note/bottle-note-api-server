package app.bottlenote.support.report.service;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.repository.ReviewReportRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static app.bottlenote.support.report.exception.ReportExceptionCode.ALREADY_REPORTED_REVIEW;

@Slf4j
@AllArgsConstructor
@Service
public class ReviewReportService {

	private final ReviewFacade reviewFacade;
	private final ReviewReportRepository reviewReportRepository;

	@Transactional
	public ReviewReportResponse reviewReport(
		Long currentUserId,
		@Valid ReviewReportRequest reviewReportRequest,
		String clientIP
	) {
		if (!reviewFacade.isExistReview(reviewReportRequest.reportReviewId())) {
			throw new ReviewException(REVIEW_NOT_FOUND);
		}
		if (reviewReportRepository.existsByUserIdAndReviewId(currentUserId, reviewReportRequest.reportReviewId())) {
			throw new ReportException(ALREADY_REPORTED_REVIEW);
		}
		ReviewReport reviewReport = ReviewReport.registerReport(
			currentUserId,
			reviewReportRequest.reportReviewId(),
			reviewReportRequest.type(),
			reviewReportRequest.content(),
			Objects.requireNonNull(clientIP)
		);
		try {
			ReviewReport saved = reviewReportRepository.save(reviewReport);
			blockReviewIfNecessary(saved.getReviewId());

		} catch (Exception e) {

			log.error("리뷰 신고 등록 중 오류가 발생했습니다. reviewReportRequest: {}", reviewReportRequest, e);
			return ReviewReportResponse.response(false);
		}
		return ReviewReportResponse.response(true);
	}

	public void blockReviewIfNecessary(Long reviewId) {
		Long count = reviewReportRepository.countUniqueIpReportsByReviewId(reviewId).orElse(0L);

		log.info("reviewId: {}, count: {}", reviewId, count);
		Review review = reviewFacade.getReview(reviewId);
		review.blockReview(count);
	}
}
