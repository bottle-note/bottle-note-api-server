package app.bottlenote.alcohols.facade.payload;

/** MFDS 매칭 등 대량 비교용 증류소 요약. */
public record DistilleryMatchTargetItem(Long id, String korName, String engName) {}
