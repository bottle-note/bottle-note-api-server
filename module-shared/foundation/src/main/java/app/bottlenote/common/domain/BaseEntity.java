package app.bottlenote.common.domain;

import jakarta.persistence.MappedSuperclass;

/** 생성자와 수정자 정보가 존재하는 기존 entity의 호환용 공통 클래스 */
@MappedSuperclass
public class BaseEntity extends BaseTimeEntity {}
