package app.bottlenote.alcohols.domain;

/** 사용자가 주류를 찜했는지 알려 주는 계약. 주류가 정하고 찜 업무가 구현한다. */
public interface AlcoholPickStatusReader {

  boolean isPicked(Long alcoholId, Long userId);
}
