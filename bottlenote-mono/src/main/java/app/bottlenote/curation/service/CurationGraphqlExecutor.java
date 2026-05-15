package app.bottlenote.curation.service;

import java.util.Map;

public interface CurationGraphqlExecutor {

  Map<String, Object> execute(Long curationId, int index, SpecGraphqlQueryBuilder.Result query);
}
