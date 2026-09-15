package app.bottlenote.campaigncontent.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface CampaignContentRepository {

  CampaignContent save(CampaignContent campaignContent);

  Optional<CampaignContent> findById(Long id);

  Optional<CampaignContent> findByCode(String code);

  boolean existsByCode(String code);

  void delete(CampaignContent campaignContent);

  /** 이름·코드 검색과 활성 필터를 적용해 최신 등록순으로 한 페이지를 조회한다. */
  List<CampaignContent> searchForAdmin(String keyword, Boolean isActive, int page, int size);

  long countForAdmin(String keyword, Boolean isActive);
}
