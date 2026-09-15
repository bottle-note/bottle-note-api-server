package app.bottlenote.campaigncontent.repository;

import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_DUPLICATE_CODE;
import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_HAS_EVENTS;

import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentRepository;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaCampaignContentRepository
    extends CampaignContentRepository, JpaRepository<CampaignContent, Long> {

  String UNIQUE_CODE_CONSTRAINT = "uk_campaign_contents_code";
  String EVENT_FOREIGN_KEY_CONSTRAINT = "fk_campaign_content_events_content";

  @Override
  default CampaignContent register(CampaignContent campaignContent) {
    try {
      return saveAndFlush(campaignContent);
    } catch (DataIntegrityViolationException exception) {
      if (CampaignContentConstraintViolationClassifier.matches(exception, UNIQUE_CODE_CONSTRAINT)) {
        throw new CampaignContentException(CAMPAIGN_CONTENT_DUPLICATE_CODE);
      }
      throw exception;
    }
  }

  @Override
  default void remove(CampaignContent campaignContent) {
    try {
      delete(campaignContent);
      flush();
    } catch (DataIntegrityViolationException exception) {
      if (CampaignContentConstraintViolationClassifier.matches(
          exception, EVENT_FOREIGN_KEY_CONSTRAINT)) {
        throw new CampaignContentException(CAMPAIGN_CONTENT_HAS_EVENTS);
      }
      throw exception;
    }
  }

  @Override
  Optional<CampaignContent> findByCode(String code);

  @Override
  boolean existsByCode(String code);

  @Query(
      """
      select c from campaign_content c
      where (:keyword is null
             or lower(c.name) like lower(concat('%', :keyword, '%'))
             or c.code like lower(concat('%', :keyword, '%')))
        and (:isActive is null or c.isActive = :isActive)
      """)
  List<CampaignContent> findAdminPage(
      @Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);

  @Query(
      """
      select count(c) from campaign_content c
      where (:keyword is null
             or lower(c.name) like lower(concat('%', :keyword, '%'))
             or c.code like lower(concat('%', :keyword, '%')))
        and (:isActive is null or c.isActive = :isActive)
      """)
  long countAdminPage(@Param("keyword") String keyword, @Param("isActive") Boolean isActive);

  // 포트에 Spring Data 페이징 타입이 새지 않도록 구현 안에서만 Pageable을 만든다.
  @Override
  default List<CampaignContent> searchForAdmin(
      String keyword, Boolean isActive, int page, int size) {
    return findAdminPage(
        keyword, isActive, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
  }

  @Override
  default long countForAdmin(String keyword, Boolean isActive) {
    return countAdminPage(keyword, isActive);
  }
}
