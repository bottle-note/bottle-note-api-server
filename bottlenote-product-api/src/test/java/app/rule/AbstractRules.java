package app.rule;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * 아키텍처 규칙 테스트의 공통 베이스.
 *
 * <p>임포트는 JVM당 한 번만 한다. `app.bottlenote` 아래 클래스 파일이 1,600개를 넘어 스캔이 비싼데, 규칙 검사는 임포트 결과를 변경하지 않으므로
 * 테스트마다 다시 읽을 이유가 없다. {@link JavaClasses}는 임포트 이후 불변이라 공유해도 안전하다.
 */
@SuppressWarnings({"JUnitTestClassNamingConvention"})
public abstract class AbstractRules {
  private static final JavaClasses IMPORTED_CLASSES =
      new ClassFileImporter().importPackages("app.bottlenote");

  static {
    // 임포트가 비면 모든 규칙이 공허하게 통과한다. 검사 대상이 실제로 있는지 확인한다.
    if (IMPORTED_CLASSES.size() == 0) {
      throw new IllegalStateException("app.bottlenote에서 검사 대상 클래스를 찾지 못했다");
    }
  }

  protected final JavaClasses importedClasses = IMPORTED_CLASSES;
}
