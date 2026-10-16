package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 화면 라벨용 속성 코드 목록. code는 요청에 쓰는 영문 코드다. */
public record AdminAlcoholAttributeEnumsResponse(
    List<AdminCodeLabelItem> types,
    List<AdminCodeLabelItem> datatypes,
    List<AdminCodeLabelItem> providers,
    List<AdminCodeLabelItem> operators) {}
