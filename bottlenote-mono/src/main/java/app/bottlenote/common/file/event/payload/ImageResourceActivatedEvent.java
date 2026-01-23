package app.bottlenote.common.file.event.payload;

import java.util.List;
import java.util.Objects;

public record ImageResourceActivatedEvent(
    List<String> resourceKeys, Long referenceId, String referenceType) {

  public ImageResourceActivatedEvent {
    Objects.requireNonNull(resourceKeys, "resourceKeys must not be null");
    Objects.requireNonNull(referenceId, "referenceId must not be null");
    Objects.requireNonNull(referenceType, "referenceType must not be null");
  }

  public static ImageResourceActivatedEvent of(
      List<String> resourceKeys, Long referenceId, String referenceType) {
    return new ImageResourceActivatedEvent(resourceKeys, referenceId, referenceType);
  }

  public static ImageResourceActivatedEvent of(
      String resourceKey, Long referenceId, String referenceType) {
    return new ImageResourceActivatedEvent(List.of(resourceKey), referenceId, referenceType);
  }
}
