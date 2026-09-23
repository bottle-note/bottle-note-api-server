package app.bottlenote.mfds.repository;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsItem;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import app.bottlenote.mfds.dto.dsl.MfdsPublicAlcoholSearchCriteria;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholCategoryItem;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
import java.util.List;
import java.util.Optional;

/** 신고 정제 데이터 목록의 keyset 커서 검색. */
public interface CustomMfdsDeclarationRepository {

  Optional<MfdsItem> findLatestItemByRcno(String rcno);

  List<MfdsDeclaration> searchByCriteria(MfdsDeclarationSearchCriteria criteria);

  /** cursor를 제외하고 목록 필터에 일치하는 전체 건수를 집계한다. */
  long countByCriteria(MfdsDeclarationSearchCriteria criteria);

  List<MfdsDeclaration> searchPublicAlcohols(MfdsPublicAlcoholSearchCriteria criteria);

  List<MfdsPublicCountryItem> findExportCountries();

  List<MfdsPublicAlcoholCategoryItem> findAlcoholCategories();
}
