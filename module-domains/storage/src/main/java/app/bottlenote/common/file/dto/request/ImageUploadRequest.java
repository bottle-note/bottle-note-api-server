package app.bottlenote.common.file.dto.request;

public record ImageUploadRequest(String rootPath, Long uploadSize, String contentType) {
  public ImageUploadRequest {
    uploadSize = uploadSize == null ? 1 : uploadSize;
    contentType = contentType == null ? "image/jpeg" : contentType;
  }
}
