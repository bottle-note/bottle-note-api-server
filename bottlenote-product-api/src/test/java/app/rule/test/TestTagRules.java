package app.rule.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("테스트 태그 정책")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
class TestTagRules {

  private static final Set<String> ALLOWED_TAGS =
      Set.of("unit", "integration", "admin_integration", "restdocs", "rule", "batch");
  private static final Pattern TAG_PATTERN =
      Pattern.compile("@Tag\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"\\s*\\)");

  @Test
  void 모든_테스트_파일은_허용된_태그를_가져야_한다() throws IOException {
    Path root = findRepositoryRoot(Path.of("").toAbsolutePath());
    List<String> violations = new ArrayList<>();

    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(Files::isRegularFile)
          .filter(TestTagRules::isTestSource)
          .forEach(path -> collectViolations(root, path, violations));
    }

    assertTrue(violations.isEmpty(), () -> "테스트 태그 정책 위반:\n- " + String.join("\n- ", violations));
  }

  private static Path findRepositoryRoot(Path start) {
    Path current = start;
    while (current != null) {
      if (Files.exists(current.resolve("settings.gradle"))) {
        return current;
      }
      current = current.getParent();
    }
    return start;
  }

  private static boolean isTestSource(Path path) {
    String normalized = path.toString().replace('\\', '/');
    String fileName = path.getFileName().toString();
    return normalized.contains("/src/test/")
        && (fileName.endsWith("Test.java") || fileName.endsWith("Test.kt"));
  }

  private static void collectViolations(Path root, Path path, List<String> violations) {
    String content;
    try {
      content = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("테스트 파일을 읽을 수 없습니다: " + path, e);
    }

    List<String> tags = new ArrayList<>();
    Matcher matcher = TAG_PATTERN.matcher(content);
    while (matcher.find()) {
      tags.add(matcher.group(1));
    }

    Path relative = root.relativize(path);
    if (tags.isEmpty()) {
      violations.add(relative + " - @Tag 누락");
      return;
    }

    tags.stream()
        .filter(tag -> !ALLOWED_TAGS.contains(tag))
        .forEach(tag -> violations.add(relative + " - 허용되지 않은 @Tag(\"" + tag + "\")"));
  }
}
