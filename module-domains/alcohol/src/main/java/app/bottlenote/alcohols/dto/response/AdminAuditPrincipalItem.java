package app.bottlenote.alcohols.dto.response;

import app.bottlenote.common.constant.AuditPrincipalType;

/** 생성·수정 주체. 도메인 임베디드 값을 응답용으로 펼친 항목이다. */
public record AdminAuditPrincipalItem(Long id, AuditPrincipalType type, String email) {}
