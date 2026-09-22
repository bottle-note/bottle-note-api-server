package app.bottlenote.review.repository;

import app.bottlenote.review.dto.request.AdminBestReviewSelectionLogSearchRequest;
import app.bottlenote.review.dto.response.AdminBestReviewSelectionLogResponse;
import org.springframework.data.domain.Page;

public interface CustomBestReviewSelectionLogRepository {

  Page<AdminBestReviewSelectionLogResponse> searchAdminLogs(
      AdminBestReviewSelectionLogSearchRequest request);
}
