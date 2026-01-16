package app.bottlenote.common.file.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceEventType {
  CREATED("리소스 생성"),
  ACTIVATED("사용 가능"),
  INVALIDATED("무효화"),
  DELETED("삭제됨");

  private final String description;
}
