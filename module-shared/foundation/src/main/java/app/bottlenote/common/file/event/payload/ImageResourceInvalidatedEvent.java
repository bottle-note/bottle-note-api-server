package app.bottlenote.common.file.event.payload;

import java.util.List;
import java.util.Objects;

public record ImageResourceInvalidatedEvent(
    List<String> resourceKeys, Long referenceId, String referenceType) {

  public ImageResourceInvalidatedEvent {
    Objects.requireNonNull(resourceKeys, "resourceKeys must not be null");
    Objects.requireNonNull(referenceId, "referenceId must not be null");
    Objects.requireNonNull(referenceType, "referenceType must not be null");
  }

  public static ImageResourceInvalidatedEvent of(
      List<String> resourceKeys, Long referenceId, String referenceType) {
    return new ImageResourceInvalidatedEvent(resourceKeys, referenceId, referenceType);
  }

  public static ImageResourceInvalidatedEvent of(
      String resourceKey, Long referenceId, String referenceType) {
    return new ImageResourceInvalidatedEvent(List.of(resourceKey), referenceId, referenceType);
  }
}
