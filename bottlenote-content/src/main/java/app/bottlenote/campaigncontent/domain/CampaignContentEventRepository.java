package app.bottlenote.campaigncontent.domain;

import app.bottlenote.common.annotation.DomainRepository;

@DomainRepository
public interface CampaignContentEventRepository {

  CampaignContentEventLog save(CampaignContentEventLog event);

  boolean existsByCampaignContentId(Long campaignContentId);
}
