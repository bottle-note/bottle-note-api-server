package app.bottlenote.common.file.event.payload;

import java.util.List;
import java.util.Objects;

public record ImageResourceDeletedEvent(
    List<String> resourceKeys, Long referenceId, String referenceType) {

  public ImageResourceDeletedEvent {
    Objects.requireNonNull(resourceKeys, "resourceKeys must not be null");
    Objects.requireNonNull(referenceId, "referenceId must not be null");
    Objects.requireNonNull(referenceType, "referenceType must not be null");
  }

  public static ImageResourceDeletedEvent of(
      List<String> resourceKeys, Long referenceId, String referenceType) {
    return new ImageResourceDeletedEvent(resourceKeys, referenceId, referenceType);
  }

  public static ImageResourceDeletedEvent of(
      String resourceKey, Long referenceId, String referenceType) {
    return new ImageResourceDeletedEvent(List.of(resourceKey), referenceId, referenceType);
  }
}
