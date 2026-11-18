package app.bottlenote.curation.service;

import java.util.Map;

public interface GraphQLCurationExecutor {

  Map<String, Object> execute(Long curationId, int index, GraphQLCurationQueryBuilder.Result query);
}
