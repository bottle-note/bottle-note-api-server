package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ALREADY_DELETED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_HAS_RATINGS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_HAS_REVIEWS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_UPDATED;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.event.payload.ImageResourceInvalidatedEvent;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.review.domain.ReviewRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAlcoholCommandService {

  private static final String REFERENCE_TYPE_ALCOHOL = "ALCOHOL";

  private final AlcoholQueryRepository alcoholQueryRepository;
  private final RegionRepository regionRepository;
  private final DistilleryRepository distilleryRepository;
  private final ReviewRepository reviewRepository;
  private final RatingRepository ratingRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public AdminResultResponse createAlcohol(AdminAlcoholUpsertRequest request) {
    Region region =
        regionRepository
            .findById(request.regionId())
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));
    Distillery distillery =
        distilleryRepository
            .findById(request.distilleryId())
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    Alcohol alcohol =
        Alcohol.builder()
            .korName(request.korName())
            .engName(request.engName())
            .abv(request.abv())
            .type(request.type())
            .korCategory(request.korCategory())
            .engCategory(request.engCategory())
            .categoryGroup(request.categoryGroup())
            .region(region)
            .distillery(distillery)
            .age(request.age())
            .cask(request.cask())
            .imageUrl(request.imageUrl())
            .description(request.description())
            .volume(request.volume())
            .build();

    Alcohol saved = alcoholQueryRepository.save(alcohol);
    publishImageActivatedEvent(request.imageUrl(), saved.getId());

    return AdminResultResponse.of(ALCOHOL_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse updateAlcohol(Long alcoholId, AdminAlcoholUpsertRequest request) {
    Alcohol alcohol =
        alcoholQueryRepository
            .findById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    if (alcohol.isDeleted()) {
      throw new AlcoholException(ALCOHOL_ALREADY_DELETED);
    }

    Region region =
        regionRepository
            .findById(request.regionId())
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));
    Distillery distillery =
        distilleryRepository
            .findById(request.distilleryId())
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    String oldImageUrl = alcohol.getImageUrl();

    alcohol.update(
        request.korName(),
        request.engName(),
        request.abv(),
        request.type(),
        request.korCategory(),
        request.engCategory(),
        request.categoryGroup(),
        region,
        distillery,
        request.age(),
        request.cask(),
        request.imageUrl(),
        request.description(),
        request.volume());

    handleImageChange(oldImageUrl, request.imageUrl(), alcoholId);

    return AdminResultResponse.of(ALCOHOL_UPDATED, alcoholId);
  }

  @Transactional
  public AdminResultResponse deleteAlcohol(Long alcoholId) {
    Alcohol alcohol =
        alcoholQueryRepository
            .findById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    if (alcohol.isDeleted()) {
      throw new AlcoholException(ALCOHOL_ALREADY_DELETED);
    }

    if (reviewRepository.existsByAlcoholId(alcoholId)) {
      throw new AlcoholException(ALCOHOL_HAS_REVIEWS);
    }

    if (ratingRepository.existsByAlcoholId(alcoholId)) {
      throw new AlcoholException(ALCOHOL_HAS_RATINGS);
    }

    alcohol.delete();
    return AdminResultResponse.of(ALCOHOL_DELETED, alcoholId);
  }

  private void publishImageActivatedEvent(String imageUrl, Long alcoholId) {
    if (imageUrl == null || imageUrl.isBlank()) return;

    String resourceKey = ImageUtil.extractResourceKey(imageUrl);
    if (resourceKey != null) {
      eventPublisher.publishEvent(
          ImageResourceActivatedEvent.of(List.of(resourceKey), alcoholId, REFERENCE_TYPE_ALCOHOL));
    }
  }

  private void handleImageChange(String oldImageUrl, String newImageUrl, Long alcoholId) {
    if (Objects.equals(oldImageUrl, newImageUrl)) return;

    if (oldImageUrl != null && !oldImageUrl.isBlank()) {
      String oldResourceKey = ImageUtil.extractResourceKey(oldImageUrl);
      if (oldResourceKey != null) {
        eventPublisher.publishEvent(
            ImageResourceInvalidatedEvent.of(
                List.of(oldResourceKey), alcoholId, REFERENCE_TYPE_ALCOHOL));
      }
    }

    publishImageActivatedEvent(newImageUrl, alcoholId);
  }
}
