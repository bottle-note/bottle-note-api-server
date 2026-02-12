package app.bottlenote.banner.service;

import static app.bottlenote.banner.exception.BannerExceptionCode.BANNER_DUPLICATE_NAME;
import static app.bottlenote.banner.exception.BannerExceptionCode.BANNER_INVALID_DATE_RANGE;
import static app.bottlenote.banner.exception.BannerExceptionCode.BANNER_NOT_FOUND;
import static app.bottlenote.banner.exception.BannerExceptionCode.BANNER_TARGET_URL_REQUIRED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.BANNER_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.BANNER_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.BANNER_SORT_ORDER_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.BANNER_STATUS_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.BANNER_UPDATED;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.banner.dto.request.AdminBannerCreateRequest;
import app.bottlenote.banner.dto.request.AdminBannerSearchRequest;
import app.bottlenote.banner.dto.request.AdminBannerSortOrderRequest;
import app.bottlenote.banner.dto.request.AdminBannerStatusRequest;
import app.bottlenote.banner.dto.request.AdminBannerUpdateRequest;
import app.bottlenote.banner.dto.response.AdminBannerDetailResponse;
import app.bottlenote.banner.exception.BannerException;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerService {

  private final BannerRepository bannerRepository;

  @Transactional(readOnly = true)
  public GlobalResponse search(AdminBannerSearchRequest request) {
    PageRequest pageable = PageRequest.of(request.page(), request.size());
    return GlobalResponse.fromPage(bannerRepository.searchForAdmin(request, pageable));
  }

  @Transactional(readOnly = true)
  public AdminBannerDetailResponse getDetail(Long bannerId) {
    Banner banner =
        bannerRepository
            .findById(bannerId)
            .orElseThrow(() -> new BannerException(BANNER_NOT_FOUND));

    return new AdminBannerDetailResponse(
        banner.getId(),
        banner.getName(),
        banner.getNameFontColor(),
        banner.getDescriptionA(),
        banner.getDescriptionB(),
        banner.getDescriptionFontColor(),
        banner.getImageUrl(),
        banner.getTextPosition(),
        banner.getIsExternalUrl(),
        banner.getTargetUrl(),
        banner.getBannerType(),
        banner.getSortOrder(),
        banner.getStartDate(),
        banner.getEndDate(),
        banner.getIsActive(),
        banner.getCreateAt(),
        banner.getLastModifyAt());
  }

  @Transactional
  public AdminResultResponse create(AdminBannerCreateRequest request) {
    if (bannerRepository.existsByName(request.name())) {
      throw new BannerException(BANNER_DUPLICATE_NAME);
    }

    validateDateRange(request.startDate(), request.endDate());
    validateExternalUrl(request.isExternalUrl(), request.targetUrl());

    boolean isActive = determineActiveStatus(true, request.endDate());

    reorderSortOrders(request.sortOrder(), null);

    Banner banner =
        Banner.builder()
            .name(request.name())
            .nameFontColor(request.nameFontColor())
            .descriptionA(request.descriptionA())
            .descriptionB(request.descriptionB())
            .descriptionFontColor(request.descriptionFontColor())
            .imageUrl(request.imageUrl())
            .textPosition(request.textPosition())
            .isExternalUrl(request.isExternalUrl())
            .targetUrl(request.targetUrl())
            .bannerType(request.bannerType())
            .sortOrder(request.sortOrder())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .isActive(isActive)
            .build();

    Banner saved = bannerRepository.save(banner);
    return AdminResultResponse.of(BANNER_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long bannerId, AdminBannerUpdateRequest request) {
    Banner banner =
        bannerRepository
            .findById(bannerId)
            .orElseThrow(() -> new BannerException(BANNER_NOT_FOUND));

    validateDateRange(request.startDate(), request.endDate());
    validateExternalUrl(request.isExternalUrl(), request.targetUrl());

    boolean isActive = determineActiveStatus(request.isActive(), request.endDate());

    if (!banner.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), banner.getId());
    }

    banner.update(
        request.name(),
        request.nameFontColor(),
        request.descriptionA(),
        request.descriptionB(),
        request.descriptionFontColor(),
        request.imageUrl(),
        request.textPosition(),
        request.isExternalUrl(),
        request.targetUrl(),
        request.bannerType(),
        request.sortOrder(),
        request.startDate(),
        request.endDate(),
        isActive);

    return AdminResultResponse.of(BANNER_UPDATED, bannerId);
  }

  @Transactional
  public AdminResultResponse delete(Long bannerId) {
    Banner banner =
        bannerRepository
            .findById(bannerId)
            .orElseThrow(() -> new BannerException(BANNER_NOT_FOUND));

    bannerRepository.delete(banner);
    return AdminResultResponse.of(BANNER_DELETED, bannerId);
  }

  @Transactional
  public AdminResultResponse updateStatus(Long bannerId, AdminBannerStatusRequest request) {
    Banner banner =
        bannerRepository
            .findById(bannerId)
            .orElseThrow(() -> new BannerException(BANNER_NOT_FOUND));

    banner.updateStatus(request.isActive());
    return AdminResultResponse.of(BANNER_STATUS_UPDATED, bannerId);
  }

  @Transactional
  public AdminResultResponse updateSortOrder(Long bannerId, AdminBannerSortOrderRequest request) {
    Banner banner =
        bannerRepository
            .findById(bannerId)
            .orElseThrow(() -> new BannerException(BANNER_NOT_FOUND));

    if (!banner.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), banner.getId());
    }

    banner.updateSortOrder(request.sortOrder());
    return AdminResultResponse.of(BANNER_SORT_ORDER_UPDATED, bannerId);
  }

  private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new BannerException(BANNER_INVALID_DATE_RANGE);
    }
  }

  private void validateExternalUrl(Boolean isExternalUrl, String targetUrl) {
    if (Boolean.TRUE.equals(isExternalUrl) && (targetUrl == null || targetUrl.isBlank())) {
      throw new BannerException(BANNER_TARGET_URL_REQUIRED);
    }
  }

  private boolean determineActiveStatus(boolean requestedIsActive, LocalDateTime endDate) {
    if (endDate != null && endDate.isBefore(LocalDateTime.now())) {
      return false;
    }
    return requestedIsActive;
  }

  private void reorderSortOrders(Integer newSortOrder, Long excludeBannerId) {
    List<Banner> conflicting = bannerRepository.findAllBySortOrderGreaterThanEqual(newSortOrder);
    conflicting.stream()
        .filter(b -> !b.getId().equals(excludeBannerId))
        .forEach(b -> b.updateSortOrder(b.getSortOrder() + 1));
  }
}
