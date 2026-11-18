package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.CategoryPairItem;
import app.bottlenote.global.data.response.GlobalResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
}
