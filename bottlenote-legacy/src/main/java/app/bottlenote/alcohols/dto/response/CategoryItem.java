package app.bottlenote.alcohols.dto.response;

import app.bottlenote.shared.alcohols.constant.AlcoholCategoryGroup;

public record CategoryItem(
    String korCategory, String engCategory, AlcoholCategoryGroup categoryGroup) {}
