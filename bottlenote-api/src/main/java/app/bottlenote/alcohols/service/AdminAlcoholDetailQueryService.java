package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse.TastingTagInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.QueryDslAdminAlcoholDetailRepository;
import app.bottlenote.alcohols.repository.QueryDslAdminAlcoholDetailRepository.AdminAlcoholDetailProjection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 주류 단건 상세 조회. 별점·리뷰·찜 집계를 함께 읽으므로 최종 소유는 admin API를 제공하는 앱(product-api)이다. */
@Service
public class AdminAlcoholDetailQueryService {
  private final QueryDslAdminAlcoholDetailRepository adminAlcoholDetailRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  public AdminAlcoholDetailQueryService(
      QueryDslAdminAlcoholDetailRepository adminAlcoholDetailRepository,
      AlcoholQueryRepository alcoholQueryRepository) {
    this.adminAlcoholDetailRepository = adminAlcoholDetailRepository;
    this.alcoholQueryRepository = alcoholQueryRepository;
  }

  @Transactional(readOnly = true)
  public AdminAlcoholDetailResponse findAdminAlcoholDetailById(Long alcoholId) {
    AdminAlcoholDetailProjection projection =
        adminAlcoholDetailRepository
            .findAdminAlcoholDetailById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    Alcohol alcohol =
        alcoholQueryRepository
            .findById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    List<TastingTagInfo> tastingTags =
        alcohol.getAlcoholsTastingTags().stream()
            .map(
                att ->
                    new TastingTagInfo(
                        att.getTastingTag().getId(),
                        att.getTastingTag().getKorName(),
                        att.getTastingTag().getEngName()))
            .collect(
                Collectors.toMap(TastingTagInfo::id, tag -> tag, (existing, ignored) -> existing))
            .values()
            .stream()
            .toList();

    return new AdminAlcoholDetailResponse(
        projection.alcoholId(),
        projection.korName(),
        projection.engName(),
        projection.imageUrl(),
        projection.type(),
        projection.korCategory(),
        projection.engCategory(),
        projection.categoryGroup(),
        projection.abv(),
        projection.age(),
        projection.cask(),
        projection.volume(),
        projection.description(),
        projection.regionId(),
        projection.korRegion(),
        projection.engRegion(),
        projection.distilleryId(),
        projection.korDistillery(),
        projection.engDistillery(),
        tastingTags,
        projection.avgRating(),
        projection.totalRatingsCount(),
        projection.reviewCount(),
        projection.pickCount(),
        projection.createdAt(),
        projection.modifiedAt());
  }
}
