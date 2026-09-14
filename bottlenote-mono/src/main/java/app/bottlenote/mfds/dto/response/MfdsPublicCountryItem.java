package app.bottlenote.mfds.dto.response;

/** 수입 주류 검색용 수출국. 원장에 등장한 ISO Alpha-2만 담는다. */
public record MfdsPublicCountryItem(String alpha2, String nameKo, String nameEn) {}
