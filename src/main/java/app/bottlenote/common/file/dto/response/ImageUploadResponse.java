package app.bottlenote.common.file.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record ImageUploadResponse(
    String bucketName, int uploadSize, Integer expiryTime, List<ImageUploadItem> imageUploadInfo) {}
