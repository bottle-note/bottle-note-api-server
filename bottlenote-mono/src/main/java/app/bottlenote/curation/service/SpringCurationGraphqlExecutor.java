package app.bottlenote.curation.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SpringCurationGraphqlExecutor implements CurationGraphqlExecutor {

  private final ExecutionGraphQlService executionGraphQlService;

  @Override
  public Map<String, Object> execute(
      Long curationId, int index, SpecGraphqlQueryBuilder.Result query) {
    ExecutionGraphQlRequest request =
        new DefaultExecutionGraphQlRequest(
            query.query(),
            "Q",
            query.variables(),
            Map.of(),
            "curation-" + curationId + "-" + index,
            null);
    ExecutionGraphQlResponse response = Mono.from(executionGraphQlService.execute(request)).block();
    if (response == null) {
      return Map.of();
    }
    return response.getExecutionResult().toSpecification();
  }
}
