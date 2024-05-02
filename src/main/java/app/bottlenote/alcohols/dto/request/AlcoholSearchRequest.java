package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AlcoholSearchRequest(
	String keyword,

	@NotNull(message = "categoryId는 필수값입니다.")
	@Min(value = 1, message = "categoryId는 1 이상의 값이어야 합니다.")
	Long categoryId,

	Long regionId,

	@NotNull(message = "정렬 타입은 필수값입니다.")
	SearchSortType sortType) {
}
