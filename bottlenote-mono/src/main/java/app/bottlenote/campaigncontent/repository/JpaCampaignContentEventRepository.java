package app.bottlenote.campaigncontent.repository;

import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND;

import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaCampaignContentEventRepository
    extends CampaignContentEventRepository, JpaRepository<CampaignContentEventLog, Long> {

  String CONTENT_FOREIGN_KEY_CONSTRAINT = "fk_campaign_content_events_content";

  @Override
  default CampaignContentEventLog register(CampaignContentEventLog event) {
    try {
      return saveAndFlush(event);
    } catch (DataIntegrityViolationException exception) {
      if (CampaignContentConstraintViolationClassifier.matches(
          exception, CONTENT_FOREIGN_KEY_CONSTRAINT)) {
        throw new CampaignContentException(CAMPAIGN_CONTENT_NOT_FOUND);
      }
      throw exception;
    }
  }

  @Override
  boolean existsByCampaignContentId(Long campaignContentId);
}
