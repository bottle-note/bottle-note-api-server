package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 둘러보기 1단계 후보 ID 추출 쿼리가 heavy 상관 서브쿼리를 포함하지 않음을 검증한다.
 *
 * <p>성능 회귀 방지 목적: 2단계 쿼리 구조(1단계 ID-only → 2단계 본문) 보장.
 */
@Tag("unit")
@SuppressWarnings({"NonAsciiCharacters"})
class ExploreStandardQueryStructureTest {

  @Test
  @DisplayName("fetchCandidateIds 메서드는 heavy 상관 서브쿼리를 호출하지 않아야 한다")
  void fetchCandidateIds_does_not_invoke_heavy_subqueries() throws IOException {
    Path path =
        Paths.get(
            "src/main/java/app/bottlenote/alcohols/repository/CustomAlcoholQueryRepositoryImpl.java");
    if (!Files.exists(path)) {
      // 테스트가 mono 루트가 아닌 상위 디렉토리에서 실행되는 경우 대비
      path = Paths.get("bottlenote-mono").resolve(path);
    }
    String source = Files.readString(path);

    String body = extractMethodBody(source, "fetchCandidateIds");

    assertThat(body)
        .as("1단계 후보 ID 쿼리에는 사용자별 heavy 상관 서브쿼리가 포함되지 않아야 한다")
        .doesNotContain("myRating(")
        .doesNotContain("averageReviewRating(")
        .doesNotContain("isPickedSubquery(")
        .doesNotContain("getTastingTags(");
  }

  /** 메서드 이름으로 간단히 바디 블록(중괄호 쌍 매칭)을 추출한다. */
  private static String extractMethodBody(String source, String methodName) {
    int nameIdx = source.indexOf(" " + methodName + "(");
    if (nameIdx < 0) {
      throw new IllegalStateException("method not found: " + methodName);
    }
    int braceStart = source.indexOf('{', nameIdx);
    if (braceStart < 0) {
      throw new IllegalStateException("method body start not found: " + methodName);
    }
    int depth = 0;
    int i = braceStart;
    for (; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '{') depth++;
      else if (c == '}') {
        depth--;
        if (depth == 0) return source.substring(braceStart, i + 1);
      }
    }
    throw new IllegalStateException("method body end not found: " + methodName);
  }
}
