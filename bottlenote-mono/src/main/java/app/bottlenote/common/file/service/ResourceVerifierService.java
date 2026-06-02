package app.bottlenote.common.file.service;

import static app.bottlenote.common.file.constant.ResourceEventType.ACTIVATED;
import static app.bottlenote.common.file.constant.ResourceEventType.CREATED;
import static app.bottlenote.common.file.exception.FileExceptionCode.INVALID_RESOURCE_URL;
import static app.bottlenote.common.file.exception.FileExceptionCode.RESOURCE_ALREADY_USED;
import static app.bottlenote.common.file.exception.FileExceptionCode.RESOURCE_NOT_FOUND;
import static app.bottlenote.common.file.exception.FileExceptionCode.RESOURCE_OWNER_MISMATCH;

import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.exception.FileException;
import app.bottlenote.common.image.ImageUtil;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceVerifierService {

  private final ResourceLogRepository resourceLogRepository;

  @Transactional(readOnly = true)
  public List<String> verifyOwnedImageResources(
      List<String> viewUrls, Long userId, Long referenceId, String referenceType) {
    return Objects.requireNonNullElse(viewUrls, Collections.<String>emptyList()).stream()
        .map(viewUrl -> verifyOwnedImageResource(viewUrl, userId, referenceId, referenceType))
        .toList();
  }

  private String verifyOwnedImageResource(
      String viewUrl, Long userId, Long referenceId, String referenceType) {
    String resourceKey = ImageUtil.extractResourceKey(viewUrl);
    if (resourceKey == null || resourceKey.isBlank()) {
      throw new FileException(INVALID_RESOURCE_URL);
    }

    ResourceLog resourceLog =
        resourceLogRepository
            .findByResourceKey(resourceKey)
            .orElseThrow(() -> new FileException(RESOURCE_NOT_FOUND));

    if (!Objects.equals(userId, resourceLog.getUserId())) {
      throw new FileException(RESOURCE_OWNER_MISMATCH);
    }
    if (!Objects.equals(viewUrl, resourceLog.getViewUrl())) {
      throw new FileException(INVALID_RESOURCE_URL);
    }
    if (!isUsable(resourceLog, referenceId, referenceType)) {
      throw new FileException(RESOURCE_ALREADY_USED);
    }
    return resourceKey;
  }

  private boolean isUsable(ResourceLog resourceLog, Long referenceId, String referenceType) {
    if (resourceLog.getEventType() == CREATED) {
      return true;
    }
    return resourceLog.getEventType() == ACTIVATED
        && Objects.equals(referenceId, resourceLog.getReferenceId())
        && Objects.equals(referenceType, resourceLog.getReferenceType());
  }
}
