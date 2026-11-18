package app.bottlenote.campaigncontent.repository;

import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaCampaignContentEventRepository
    extends CampaignContentEventRepository, JpaRepository<CampaignContentEventLog, Long> {

  @Override
  boolean existsByCampaignContentId(Long campaignContentId);
}
