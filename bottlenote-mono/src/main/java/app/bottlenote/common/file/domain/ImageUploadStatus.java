package app.bottlenote.common.file.domain;

public enum ImageUploadStatus {
  PENDING, // URL 발급됨, 업로드 대기
  UPLOADED, // 업로드 완료, 사용 대기
  CONFIRMED, // 사용 확정
  DELETED // 삭제됨
}
