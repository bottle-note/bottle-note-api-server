package app.bottlenote.alcohols.dto.response;

import app.bottlenote.shared.constant.alcohol.AlcoholCategoryGroup;

public record CategoryItem(
    String korCategory, String engCategory, AlcoholCategoryGroup categoryGroup) {}
