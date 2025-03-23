package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup;

public record CategoryItem(
	String korCategory,
	String engCategory,
	AlcoholCategoryGroup categoryGroup
) {
}
