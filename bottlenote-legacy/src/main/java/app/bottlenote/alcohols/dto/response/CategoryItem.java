package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;

public record CategoryItem(
    String korCategory, String engCategory, AlcoholCategoryGroup categoryGroup) {}
