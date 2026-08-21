package app.bottlenote.mfds.repository;

import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import java.util.List;

/** 수입사 목록의 keyset 커서 검색. */
public interface CustomMfdsImporterRepository {

  List<MfdsImporter> searchByCriteria(MfdsImporterSearchCriteria criteria);

  /** cursor를 제외하고 목록 필터에 일치하는 전체 건수를 집계한다. */
  long countByCriteria(MfdsImporterSearchCriteria criteria);
}
