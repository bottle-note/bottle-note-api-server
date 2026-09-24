package app.bottlenote.mfds.dto.response;

/** 일괄 매칭 분류 이유. code는 검증과 화면 분기에 쓰고 message는 관리자에게 보여 준다. */
public record MfdsBulkMatchingReasonItem(String code, String message) {}
