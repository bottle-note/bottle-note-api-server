package app.bottlenote.review.service;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.BestReviewSelectionLogRepository;
import app.bottlenote.review.dto.request.AdminBestReviewSelectionLogSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBestReviewSelectionLogQueryService {

  private final BestReviewSelectionLogRepository bestReviewSelectionLogRepository;

  @Transactional(readOnly = true)
  public GlobalResponse searchLogs(AdminBestReviewSelectionLogSearchRequest request) {
    return GlobalResponse.fromPage(bestReviewSelectionLogRepository.searchAdminLogs(request));
  }
}
