package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_NOT_FOUND;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_NOT_FOUND;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationFeedItemResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationListResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      Long cursor, Integer size) {
    int pageSize = normalizeFeedSize(size);
    long currentCursor = cursor != null && cursor > 0 ? cursor : 0L;
    List<Curation> visibleCurations = curationRepository.findAllVisibleOn(LocalDate.now());
    int fromIndex = Math.min(Math.toIntExact(currentCursor), visibleCurations.size());
    int toIndex = Math.min(fromIndex + pageSize + 1, visibleCurations.size());
    List<Curation> pageContent = new ArrayList<>(visibleCurations.subList(fromIndex, toIndex));
    Map<Long, CurationSpec> specMap =
        curationSpecRepository
            .findAllByIdIn(
                pageContent.stream().map(Curation::getSpecId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(CurationSpec::getId, Function.identity()));
    List<ProductSpecBasedCurationFeedItemResponse> items =
        pageContent.stream()
            .map(curation -> toFeedResponse(curation, specMap.get(curation.getSpecId())))
            .toList();
    return CursorResponse.of(items, currentCursor, pageSize);
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
      Curation curation, CurationSpec spec) {
    CurationExtension extension =
        curationExtensionRepository
            .findByCurationId(curation.getId())
            .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
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
