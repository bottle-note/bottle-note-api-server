package app.bottlenote.curation.repository;

import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import java.util.List;

public interface CustomCurationFeedRepository {

  List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria);
}
