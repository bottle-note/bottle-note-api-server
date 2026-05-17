package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_GRAPHQL_EXECUTION_FAILED;

import app.bottlenote.curation.exception.CurationException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringCurationGraphqlExecutor implements CurationGraphqlExecutor {

  private static final Duration GRAPHQL_EXECUTION_TIMEOUT = Duration.ofSeconds(3);

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
    ExecutionGraphQlResponse response;
    try {
      response =
          Mono.from(executionGraphQlService.execute(request)).block(GRAPHQL_EXECUTION_TIMEOUT);
    } catch (RuntimeException e) {
      log.error(
          "Curation GraphQL hydration execution failed. curationId={}, queryIndex={}, payloadPath={}, entryField={}, timeout={}",
          curationId,
          index,
          query.payloadPath(),
          query.entryField(),
          GRAPHQL_EXECUTION_TIMEOUT,
          e);
      throw new CurationException(CURATION_GRAPHQL_EXECUTION_FAILED);
    }
    if (response == null) {
      log.error(
          "Curation GraphQL hydration returned null response. curationId={}, queryIndex={}, payloadPath={}, entryField={}",
          curationId,
          index,
          query.payloadPath(),
          query.entryField());
      throw new CurationException(CURATION_GRAPHQL_EXECUTION_FAILED);
    }
    if (!response.getExecutionResult().getErrors().isEmpty()) {
      log.error(
          "Curation GraphQL hydration returned errors. curationId={}, queryIndex={}, payloadPath={}, entryField={}, errors={}",
          curationId,
          index,
          query.payloadPath(),
          query.entryField(),
          response.getExecutionResult().getErrors());
      throw new CurationException(CURATION_GRAPHQL_EXECUTION_FAILED);
    }
    return response.getExecutionResult().toSpecification();
  }
}
