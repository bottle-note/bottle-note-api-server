package app.bottlenote.alcohols.domain;

/** 중복 위스키 하나를 병합할 때 대상(name)별로 기준 위스키로 옮긴 건수와 삭제한 건수. */
public record AlcoholMergeCount(String name, int moved, int discarded) {}
