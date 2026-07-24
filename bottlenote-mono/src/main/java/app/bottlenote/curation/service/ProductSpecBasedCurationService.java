package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_NOT_FOUND;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_NOT_FOUND;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationFeedItemResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationListResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSpecBasedCurationService {

  private static final String DEFAULT_CONTAINER = "object";
  private static final int MAX_FEED_SIZE = 10;

  private final CurationRepository curationRepository;
  private final CurationSpecRepository curationSpecRepository;
  private final CurationExtensionRepository curationExtensionRepository;
  private final CurationResponseMaterializer responseMaterializer;
  private final CurationFeedProjector feedProjector;

  @Transactional(readOnly = true)
  public List<ProductSpecBasedCurationListResponse> listActiveCurations() {
    List<Curation> curations = curationRepository.findAllVisibleOn(LocalDate.now());
    Map<Long, CurationSpec> specMap =
        curationSpecRepository
            .findAllByIdIn(curations.stream().map(Curation::getSpecId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(CurationSpec::getId, Function.identity()));
    return curations.stream()
        .map(curation -> toListResponse(curation, specMap.get(curation.getSpecId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public CursorResponse<ProductSpecBasedCurationFeedItemResponse> searchFeed(
      String keyword, List<String> codes, Long cursor, Integer size) {
    int pageSize = normalizeFeedSize(size);
    long currentCursor = cursor != null && cursor > 0 ? cursor : 0L;
    List<String> normalizedCodes = normalizeCodes(codes);
    List<CurationSpec> specs =
        normalizedCodes.isEmpty()
            ? List.of()
            : curationSpecRepository.findAllByCodeIn(normalizedCodes);
    Map<Long, CurationSpec> specMap =
        specs.stream().collect(Collectors.toMap(CurationSpec::getId, Function.identity()));
    Set<Long> keywordMatchedSpecIds =
        specs.stream()
            .filter(spec -> matchesKeyword(spec, keyword))
            .map(CurationSpec::getId)
            .collect(Collectors.toSet());
    CurationFeedSearchCriteria criteria =
        new CurationFeedSearchCriteria(
            keyword,
            specMap.keySet(),
            keywordMatchedSpecIds,
            LocalDate.now(),
            currentCursor,
            pageSize + 1);
    List<Long> candidateIds = curationRepository.findFeedCandidateIds(criteria);
    if (candidateIds.isEmpty()) {
      return CursorResponse.of(
          List.of(), CursorPageable.of(candidateIds, currentCursor, Integer.valueOf(pageSize)));
    }
    List<Long> pageIds =
        candidateIds.size() > pageSize ? candidateIds.subList(0, pageSize) : candidateIds;
    Map<Long, Curation> curationMap =
        curationRepository.findAllByIdIn(pageIds).stream()
            .collect(Collectors.toMap(Curation::getId, Function.identity()));
    Map<Long, CurationExtension> extensionMap =
        curationExtensionRepository.findAllByCurationIdIn(pageIds).stream()
            .collect(Collectors.toMap(CurationExtension::getCurationId, Function.identity()));
    List<ProductSpecBasedCurationFeedItemResponse> items =
        pageIds.stream()
            .map(
                curationId -> {
                  Curation curation = requireValue(curationMap.get(curationId), CURATION_NOT_FOUND);
                  CurationSpec spec =
                      requireValue(specMap.get(curation.getSpecId()), CURATION_SPEC_NOT_FOUND);
                  CurationExtension extension =
                      requireValue(extensionMap.get(curationId), CURATION_NOT_FOUND);
                  return toFeedResponse(curation, spec, extension);
                })
            .toList();
    return CursorResponse.of(
        items, CursorPageable.of(candidateIds, currentCursor, Integer.valueOf(pageSize)));
  }

  @Transactional(readOnly = true)
  public ProductSpecBasedCurationDetailResponse getDetail(Long curationId) {
    Curation curation =
        curationRepository
            .findVisibleById(curationId, LocalDate.now())
            .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
    CurationSpec spec =
        curationSpecRepository
            .findById(curation.getSpecId())
            .orElseThrow(() -> new CurationException(CURATION_SPEC_NOT_FOUND));
    CurationExtension extension =
        curationExtensionRepository
            .findByCurationId(curationId)
            .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
    Object materialized =
        responseMaterializer.materialize(
            curationId, spec.getCode(), spec.getResponseSpec(), extension.getPayload());
    return toDetailResponse(curation, spec, materialized);
  }

  private ProductSpecBasedCurationListResponse toListResponse(
      Curation curation, CurationSpec spec) {
    return new ProductSpecBasedCurationListResponse(
        curation.getId(),
        curation.getSpecId(),
        spec != null ? spec.getCode() : null,
        spec != null ? spec.getName() : null,
        curation.getName(),
        curation.getDescription(),
        curation.getCoverImageUrl(),
        imageUrls(curation),
        curation.getExposureStartDate(),
        curation.getExposureEndDate(),
        curation.getDisplayOrder(),
        curation.getCreateAt());
  }

  private ProductSpecBasedCurationDetailResponse toDetailResponse(
      Curation curation, CurationSpec spec, Object payload) {
    return new ProductSpecBasedCurationDetailResponse(
        curation.getId(),
        curation.getName(),
        curation.getDescription(),
        curation.getCoverImageUrl(),
        imageUrls(curation),
        curation.getExposureStartDate(),
        curation.getExposureEndDate(),
        curation.getDisplayOrder(),
        curation.getCreateAt(),
        new ProductSpecBasedCurationDetailResponse.SpecMeta(
            spec.getId(), spec.getCode(), spec.getName(), container(spec), spec.getResponseSpec()),
        payload);
  }

  private ProductSpecBasedCurationFeedItemResponse toFeedResponse(
      Curation curation, CurationSpec spec, CurationExtension extension) {
    Object materialized =
        responseMaterializer.materializeFeed(
            curation.getId(), spec.getCode(), spec.getResponseSpec(), extension.getPayload());
    return new ProductSpecBasedCurationFeedItemResponse(
        curation.getId(),
        curation.getName(),
        curation.getDescription(),
        curation.getCoverImageUrl(),
        imageUrls(curation),
        curation.getExposureStartDate(),
        curation.getExposureEndDate(),
        curation.getDisplayOrder(),
        curation.getCreateAt(),
        feedProjector.projectPayload(spec.getResponseSpec(), materialized));
  }

  private List<String> imageUrls(Curation curation) {
    return java.util.stream.Stream.of(
            curation.getCoverImageUrl(), curation.getImageUrl2(), curation.getImageUrl3())
        .filter(Objects::nonNull)
        .filter(imageUrl -> !imageUrl.isBlank())
        .toList();
  }

  private boolean matchesKeyword(CurationSpec spec, String keyword) {
    if (isBlank(keyword)) {
      return false;
    }
    String normalizedKeyword = keyword.trim();
    return contains(spec.getName(), normalizedKeyword)
        || contains(spec.getDescription(), normalizedKeyword);
  }

  private boolean contains(String value, String keyword) {
    return value != null && value.contains(keyword);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private List<String> normalizeCodes(List<String> codes) {
    if (codes == null) {
      return List.of();
    }
    return codes.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(code -> !code.isBlank())
        .distinct()
        .toList();
  }

  private <T> T requireValue(T value, CurationExceptionCode code) {
    if (value == null) {
      throw new CurationException(code);
    }
    return value;
  }

  private int normalizeFeedSize(Integer size) {
    if (size == null || size < 1) {
      return MAX_FEED_SIZE;
    }
    return Math.min(size, MAX_FEED_SIZE);
  }

  private String container(CurationSpec spec) {
    Object requestContainer = spec.getRequestSpec().get("x-container");
    if (requestContainer != null) {
      return requestContainer.toString();
    }
    Object responseContainer = spec.getResponseSpec().get("x-container");
    return responseContainer != null ? responseContainer.toString() : DEFAULT_CONTAINER;
  }
}
