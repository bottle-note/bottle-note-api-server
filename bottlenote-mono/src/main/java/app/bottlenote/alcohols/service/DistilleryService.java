package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_HAS_ALCOHOLS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_UPDATED;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.request.AdminDistilleryUpsertRequest;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.dto.response.AdminResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistilleryService {

  private final DistilleryRepository distilleryRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Transactional(readOnly = true)
  public AdminDistilleryItem getDetail(Long distilleryId) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));
    return toAdminDistilleryItem(distillery);
  }

  @Transactional
  public AdminResultResponse create(AdminDistilleryUpsertRequest request) {
    if (distilleryRepository.existsByKorName(request.korName())
        || distilleryRepository.existsByEngName(request.engName())) {
      throw new AlcoholException(DISTILLERY_DUPLICATE_NAME);
    }

    Distillery distillery =
        Distillery.builder()
            .korName(request.korName())
            .engName(request.engName())
            .logoImgPath(request.logoImgUrl())
            .build();

    Distillery saved = distilleryRepository.save(distillery);
    return AdminResultResponse.of(DISTILLERY_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long distilleryId, AdminDistilleryUpsertRequest request) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    if (distilleryRepository.existsByKorNameAndIdNot(request.korName(), distilleryId)
        || distilleryRepository.existsByEngNameAndIdNot(request.engName(), distilleryId)) {
      throw new AlcoholException(DISTILLERY_DUPLICATE_NAME);
    }

    distillery.update(request.korName(), request.engName(), request.logoImgUrl());

    return AdminResultResponse.of(DISTILLERY_UPDATED, distilleryId);
  }

  @Transactional
  public AdminResultResponse delete(Long distilleryId) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    if (Boolean.TRUE.equals(alcoholQueryRepository.existsByDistilleryId(distilleryId))) {
      throw new AlcoholException(DISTILLERY_HAS_ALCOHOLS);
    }

    distilleryRepository.delete(distillery);
    return AdminResultResponse.of(DISTILLERY_DELETED, distilleryId);
  }

  private AdminDistilleryItem toAdminDistilleryItem(Distillery d) {
    return new AdminDistilleryItem(
        d.getId(),
        d.getKorName(),
        d.getEngName(),
        d.getLogoImgPath(),
        d.getCreateAt(),
        d.getLastModifyAt());
  }
}
