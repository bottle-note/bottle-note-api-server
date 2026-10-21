package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest.Condition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AlcoholCandidateCursorContext {
  private final ObjectMapper mapper;

  String context(Long groupId, AlcoholCandidateRequest r) {
    try {
      return "alcohol.candidates:"
          + mapper.writeValueAsString(
              new FilterContext(
                  groupId,
                  r.keyword(),
                  r.include(),
                  r.exclude(),
                  r.categoryGroups(),
                  r.abvFrom(),
                  r.abvTo(),
                  r.tastingGroupCodes()));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot serialize candidate filter context", e);
    }
  }

  private record FilterContext(
      Long groupId,
      String keyword,
      List<Condition> include,
      List<Condition> exclude,
      List<AlcoholCategoryGroup> categoryGroups,
      BigDecimal abvFrom,
      BigDecimal abvTo,
      List<String> tastingGroupCodes) {}
}
