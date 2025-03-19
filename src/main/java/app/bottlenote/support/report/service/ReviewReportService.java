package app.bottlenote.support.report.service;

import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.repository.ReviewReportRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static app.bottlenote.support.report.exception.ReportExceptionCode.ALREADY_REPORTED_REVIEW;

@Slf4j
@AllArgsConstructor
@Service
public class ReviewReportService {

	private static final long BLOCK_THRESHOLD = 5L;
	private final ReviewFacade reviewFacade;
	private final ReviewReportRepository reviewReportRepository;

	@Transactional
	public ReviewReportResponse reviewReport(
		Long currentUserId,
		ReviewReportRequest reviewReportRequest,
		String clientIP
	) {
		final Long reportReviewId = reviewReportRequest.reportReviewId();
		if (!reviewFacade.isExistReview(reportReviewId)) {
			throw new ReviewException(REVIEW_NOT_FOUND);
		}
		reviewReportRepository.findByUserIdAndReviewId(currentUserId, reportReviewId)
			.ifPresentOrElse(reviewReport -> {
					throw new ReportException(ALREADY_REPORTED_REVIEW);
				}, () -> {
					final String safeClientIp = Objects.requireNonNull(clientIP);
					ReviewReport reviewReport = ReviewReport.registerReport(
						currentUserId,
						reportReviewId,
						reviewReportRequest.type(),
						reviewReportRequest.content(),
						safeClientIp
					);
					ReviewReport saved = reviewReportRepository.save(reviewReport);
					blockReviewIfNecessary(saved.getReviewId());
				}
			);
		return ReviewReportResponse.response(true);
	}

	@Transactional
	public void blockReviewIfNecessary(Long reviewId) {
		Long count = reviewReportRepository.countUniqueIpReportsByReviewId(reviewId).orElse(0L);
		if (count >= BLOCK_THRESHOLD) {
			reviewFacade.requestBlockReview(reviewId);
		}
	}
}
