package app.bottlenote.alcohols.domain;

/** 병합 한 건에서 대상(name)별로 기준 주류로 옮긴 건수와 겹쳐서 삭제한 건수. */
public record AlcoholMergeCount(String name, int moved, int discarded) {}
