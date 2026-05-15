package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_NOT_FOUND;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_PAYLOAD_INVALID;
import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_SPEC_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_UPDATED;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSearchRequest;
import app.bottlenote.curation.dto.request.CurationUpdateRequest;
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationListResponse;
import app.bottlenote.curation.dto.response.CurationSpecResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSpecBasedCurationService {

  private final CurationSpecRepository curationSpecRepository;
  private final CurationRepository curationRepository;
  private final CurationExtensionRepository curationExtensionRepository;
  private final CurationPayloadValidator curationPayloadValidator;

  @Transactional(readOnly = true)
  public List<CurationSpecResponse> listSpecs() {
    return curationSpecRepository.findAllByIsActiveTrueOrderByIdAsc().stream()
        .map(this::toSpecResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CurationSpecResponse getSpecDetail(Long specId) {
    return toSpecResponse(getSpec(specId));
  }

  @Transactional(readOnly = true)
  public GlobalResponse search(CurationSearchRequest request) {
    PageRequest pageable = PageRequest.of(request.page(), request.size());
    Page<Curation> page =
        curationRepository.searchForAdmin(request.keyword(), request.isActive(), pageable);
    Map<Long, CurationSpec> specMap =
        curationSpecRepository
            .findAllByIdIn(
                page.getContent().stream().map(Curation::getSpecId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(CurationSpec::getId, Function.identity()));
    return GlobalResponse.fromPage(
        page.map(curation -> toListResponse(curation, specMap.get(curation.getSpecId()))));
  }

  @Transactional(readOnly = true)
  public AdminSpecBasedCurationDetailResponse getDetail(Long curationId) {
    Curation curation = getCuration(curationId);
    CurationSpec spec = getSpec(curation.getSpecId());
    CurationExtension extension = getExtension(curationId);
    return toDetailResponse(curation, spec, extension);
  }

  @Transactional
  public AdminResultResponse create(CurationCreateRequest request) {
    CurationSpec spec = getSpec(request.specId());
    validatePayload(spec, request.payload());
    Curation saved = curationRepository.save(toCuration(request, spec));
    curationExtensionRepository.save(
        CurationExtension.builder()
            .curationId(saved.getId())
            .specId(spec.getId())
            .payload(request.payload())
            .build());
    return AdminResultResponse.of(CURATION_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long curationId, CurationUpdateRequest request) {
    Curation curation = getCuration(curationId);
    CurationSpec spec = getSpec(request.specId());
    validatePayload(spec, request.payload());
    curation.update(
        spec.getId(),
        request.name(),
        request.description(),
        request.imageUrls().get(0),
        request.imageUrls().size() > 1 ? request.imageUrls().get(1) : null,
        request.imageUrls().size() > 2 ? request.imageUrls().get(2) : null,
        request.exposureStartDate(),
        request.exposureEndDate(),
        request.displayOrder(),
        request.isActive());
    getExtension(curationId).update(spec.getId(), request.payload());
    return AdminResultResponse.of(CURATION_UPDATED, curationId);
  }

  private Curation toCuration(CurationCreateRequest request, CurationSpec spec) {
    return Curation.builder()
        .specId(spec.getId())
        .name(request.name())
        .description(request.description())
        .coverImageUrl(request.imageUrls().get(0))
        .imageUrl2(request.imageUrls().size() > 1 ? request.imageUrls().get(1) : null)
        .imageUrl3(request.imageUrls().size() > 2 ? request.imageUrls().get(2) : null)
        .exposureStartDate(request.exposureStartDate())
        .exposureEndDate(request.exposureEndDate())
        .displayOrder(request.displayOrder())
        .isActive(request.isActive())
        .build();
  }

  private Curation getCuration(Long curationId) {
    return curationRepository
        .findById(curationId)
        .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
  }

  private CurationSpec getSpec(Long specId) {
    return curationSpecRepository
        .findById(specId)
        .orElseThrow(() -> new CurationException(CURATION_SPEC_NOT_FOUND));
  }

  private CurationExtension getExtension(Long curationId) {
    return curationExtensionRepository
        .findByCurationId(curationId)
        .orElseThrow(() -> new CurationException(CURATION_NOT_FOUND));
  }

  private void validatePayload(CurationSpec spec, Object payload) {
    List<String> errors =
        curationPayloadValidator.validate(new MapBackedSchema(spec.getRequestSpec()), payload);
    if (!errors.isEmpty()) {
      throw new CurationException(CURATION_PAYLOAD_INVALID);
    }
  }

  private AdminSpecBasedCurationListResponse toListResponse(Curation curation, CurationSpec spec) {
    return new AdminSpecBasedCurationListResponse(
        curation.getId(),
        curation.getSpecId(),
        spec != null ? spec.getCode() : null,
        curation.getName(),
        curation.getDisplayOrder(),
        curation.getIsActive(),
        curation.getCreateAt());
  }

  private AdminSpecBasedCurationDetailResponse toDetailResponse(
      Curation curation, CurationSpec spec, CurationExtension extension) {
    return new AdminSpecBasedCurationDetailResponse(
        curation.getId(),
        curation.getName(),
        curation.getDescription(),
        curation.getCoverImageUrl(),
        imageUrls(curation),
        curation.getExposureStartDate(),
        curation.getExposureEndDate(),
        curation.getDisplayOrder(),
        curation.getIsActive(),
        curation.getCreateAt(),
        curation.getLastModifyAt(),
        toSpecResponse(spec),
        extension.getPayload());
  }

  private List<String> imageUrls(Curation curation) {
    return java.util.stream.Stream.of(
            curation.getCoverImageUrl(), curation.getImageUrl2(), curation.getImageUrl3())
        .filter(Objects::nonNull)
        .filter(imageUrl -> !imageUrl.isBlank())
        .toList();
  }

  private CurationSpecResponse toSpecResponse(CurationSpec spec) {
    return new CurationSpecResponse(
        spec.getId(),
        spec.getCode(),
        spec.getName(),
        spec.getDescription(),
        spec.getHydratorKey(),
        spec.getVersion(),
        spec.getIsActive(),
        spec.getRequestSpec(),
        spec.getResponseSpec());
  }
}
