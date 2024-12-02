package app.bottlenote.support.report.service;

import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.repository.ReviewReportRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

@Slf4j
@AllArgsConstructor
@Service
public class ReviewReportService {

	private final ReviewFacade reviewFacade;
	private final ReviewReportRepository reviewReportRepository;

	public ReviewReportResponse reviewReport(Long currentUserId, @Valid ReviewReportRequest reviewReportRequest) {
		if (!reviewFacade.isExistReview(reviewReportRequest.reportReviewId())) {
			throw new ReviewException(REVIEW_NOT_FOUND);
		}
		ReviewReport reviewReport = ReviewReport.registerReport(
			currentUserId,
			reviewReportRequest.reportReviewId(),
			reviewReportRequest.content(),
			reviewReportRequest.type()
		);
		try {
			reviewReportRepository.save(reviewReport);
			
		} catch (Exception e) {
			log.error("리뷰 신고 등록 중 오류가 발생했습니다. reviewReportRequest: {}", reviewReportRequest, e);
			return ReviewReportResponse.response(false);
		}
		return ReviewReportResponse.response(true);
	}
}
