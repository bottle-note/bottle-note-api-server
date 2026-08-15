package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse.TastingTagInfo;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.CategoryPairItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.CustomAlcoholQueryRepository.AdminAlcoholDetailProjection;
import app.bottlenote.global.data.response.GlobalResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAlcoholQueryService {
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Transactional(readOnly = true)
  public GlobalResponse searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    return GlobalResponse.fromPage(alcoholQueryRepository.searchAdminAlcohols(request));
  }

  @Transactional(readOnly = true)
  public Map<AlcoholCategoryGroup, List<CategoryPairItem>> findAllCategoryReferenceMap() {
    Map<AlcoholCategoryGroup, List<CategoryPairItem>> grouped =
        new EnumMap<>(AlcoholCategoryGroup.class);
    for (AlcoholCategoryGroup group : AlcoholCategoryGroup.values()) {
      grouped.put(group, new ArrayList<>());
    }

    for (CategoryItem item : alcoholQueryRepository.findAllCategoryItems()) {
      AlcoholCategoryGroup group = item.categoryGroup();
      if (group == null) continue;
      grouped.get(group).add(new CategoryPairItem(item.korCategory(), item.engCategory()));
    }

    return grouped;
  }

  @Transactional(readOnly = true)
  public AdminAlcoholDetailResponse findAdminAlcoholDetailById(Long alcoholId) {
    AdminAlcoholDetailProjection projection =
        alcoholQueryRepository
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
