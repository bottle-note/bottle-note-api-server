package app.bottlenote.review.domain;

import app.bottlenote.review.dto.request.AdminBestReviewSelectionLogSearchRequest;
import app.bottlenote.review.dto.response.AdminBestReviewSelectionLogResponse;
import org.springframework.data.domain.Page;

public interface BestReviewSelectionLogRepository {

  BestReviewSelectionLog save(BestReviewSelectionLog log);

  Page<AdminBestReviewSelectionLogResponse> searchAdminLogs(
      AdminBestReviewSelectionLogSearchRequest request);
}
