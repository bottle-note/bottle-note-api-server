package app.bottlenote.review.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.review.domain.BestReviewSelectionLog;
import app.bottlenote.review.domain.BestReviewSelectionLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaBestReviewSelectionLogRepository
    extends BestReviewSelectionLogRepository,
        CustomBestReviewSelectionLogRepository,
        JpaRepository<BestReviewSelectionLog, Long> {}
