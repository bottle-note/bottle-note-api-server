package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholAttributeSummaryItem(
    AdminAlcoholAttributeItem attribute,
    List<Long> alcoholIds,
    List<Long> mappingIds,
    List<AdminAlcoholAttributeMappingItem> mappings,
    long registeredCount,
    long selectedCount) {}
