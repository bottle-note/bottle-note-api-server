package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminAlcoholUpsertRequest(
    @NotBlank(message = "한글 이름은 필수입니다.") String korName,
    @NotBlank(message = "영문 이름은 필수입니다.") String engName,
    @NotBlank(message = "도수는 필수입니다.") String abv,
    @NotNull(message = "주류 타입은 필수입니다.") AlcoholType type,
    @NotBlank(message = "한글 카테고리는 필수입니다.") String korCategory,
    @NotBlank(message = "영문 카테고리는 필수입니다.") String engCategory,
    @NotNull(message = "카테고리 그룹은 필수입니다.") AlcoholCategoryGroup categoryGroup,
    @NotNull(message = "지역 ID는 필수입니다.") Long regionId,
    @NotNull(message = "증류소 ID는 필수입니다.") Long distilleryId,
    @NotBlank(message = "숙성년도는 필수입니다.") String age,
    @NotBlank(message = "캐스크 타입은 필수입니다.") String cask,
    @NotBlank(message = "이미지 URL은 필수입니다.") String imageUrl,
    @NotBlank(message = "설명은 필수입니다.") String description,
    @NotBlank(message = "용량은 필수입니다.") String volume) {}
