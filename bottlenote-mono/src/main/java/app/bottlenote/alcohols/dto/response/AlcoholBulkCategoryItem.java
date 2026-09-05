package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;

public record AlcoholBulkCategoryItem(
    String korCategory, String engCategory, AlcoholCategoryGroup categoryGroup, AlcoholType type) {}
