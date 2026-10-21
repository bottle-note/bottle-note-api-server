package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.Source;
import java.math.BigDecimal;
import java.util.List;

public record AdminAlcoholCandidateResponse(
    CandidateGroup group,
    List<CandidateAttributeDefinition> attributeDefinitions,
    List<CandidateItem> items,
    long totalCount,
    CandidateFacets facets) {
  public record CandidateGroup(Long id, String name) {}

  public record CandidateNamedItem(Long id, String korName, String engName) {}

  public record CandidateItem(
      Long alcoholId,
      String korName,
      String engName,
      String imageUrl,
      AlcoholType type,
      AlcoholCategoryGroup categoryGroup,
      String korCategory,
      String engCategory,
      String abvRaw,
      BigDecimal abv,
      String age,
      String cask,
      String volume,
      CandidateNamedItem region,
      CandidateNamedItem distillery,
      List<CandidateTag> tastingTags,
      List<String> tastingGroupCodes,
      List<CandidateAttributeValues> attributes) {}

  public record CandidateTag(Long id, String korName, String engName, Long parentId) {}

  public record CandidateAttributeValues(Long attributeId, List<String> values) {}

  public record CandidateAttributeDefinition(
      Long attributeId,
      String key,
      String displayName,
      AttributeType type,
      AttributeDatatype datatype,
      String unit,
      boolean allowMultipleValues,
      List<String> allowedValues) {}

  public record CandidateFacets(
      Source source,
      Long attributeId,
      List<CandidateOption> options,
      boolean hasMoreOptions,
      long missingCount,
      BigDecimal min,
      BigDecimal max) {}

  public record CandidateOption(
      String value, String label, BigDecimal from, BigDecimal to, long count) {}
}
