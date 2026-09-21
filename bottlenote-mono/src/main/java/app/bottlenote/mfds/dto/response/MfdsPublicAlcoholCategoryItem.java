package app.bottlenote.mfds.dto.response;

/** 수입 주류 검색용 카테고리. 원장에 등장한 ko/en 조합과 공개 건수를 담는다. */
public record MfdsPublicAlcoholCategoryItem(
    String alcoholCategoryKo, String alcoholCategoryEn, long count) {}
