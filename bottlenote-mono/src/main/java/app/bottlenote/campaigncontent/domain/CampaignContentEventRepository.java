package app.bottlenote.campaigncontent.domain;

import app.bottlenote.common.annotation.DomainRepository;

@DomainRepository
public interface CampaignContentEventRepository {

  CampaignContentEventLog register(CampaignContentEventLog event);

  boolean existsByCampaignContentId(Long campaignContentId);
}
