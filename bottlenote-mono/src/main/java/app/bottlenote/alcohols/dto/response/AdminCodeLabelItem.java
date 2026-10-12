package app.bottlenote.alcohols.dto.response;

/** enum 값을 화면 표기와 함께 내려주는 공용 항목. 서버는 code만 관리하고 한글 표기는 enum 필드에서 가져온다. */
public record AdminCodeLabelItem(String code, String korName, String engName, String description) {}
