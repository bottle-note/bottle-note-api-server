package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTastingTagDetailResponse(
    Long id,
    String korName,
    String engName,
    String icon,
    String description,
    AdminTastingTagItem parent,
    List<AdminTastingTagItem> ancestors,
    List<AdminTastingTagItem> children,
    List<AdminAlcoholItem> alcohols,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {

  public static AdminTastingTagDetailResponse of(
      AdminTastingTagItem tagItem,
      AdminTastingTagItem parent,
      List<AdminTastingTagItem> ancestors,
      List<AdminTastingTagItem> children,
      List<AdminAlcoholItem> alcohols) {
    return new AdminTastingTagDetailResponse(
        tagItem.id(),
        tagItem.korName(),
        tagItem.engName(),
        tagItem.icon(),
        tagItem.description(),
        parent,
        ancestors,
        children,
        alcohols,
        tagItem.createdAt(),
        tagItem.modifiedAt());
  }
}
