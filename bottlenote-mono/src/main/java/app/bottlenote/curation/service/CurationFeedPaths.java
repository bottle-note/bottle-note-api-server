package app.bottlenote.curation.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

// x-feed 경로 해석 규칙. 저장 시 추출과 조회 시 보강이 같은 기준을 쓰도록 한 곳에 모은다.
final class CurationFeedPaths {

  private static final String FEED_META = "x-feed";
  private static final String PROPERTIES = "properties";
  private static final String ITEMS = "items";
  private static final String CONTAINER = "x-container";
  private static final String ARRAY = "array";
  private static final String JSON_PATH_ROOT = "$";
  private static final String JSON_PATH_PREFIX = "$.";

  private CurationFeedPaths() {}

  // x-container=array 스펙은 items가 단건 스키마다.
  static JsonNode rootSchema(JsonNode specNode) {
    if (isArrayContainer(specNode) && specNode.has(ITEMS)) {
      return specNode.get(ITEMS);
    }
    return specNode;
  }

  static boolean isArrayContainer(JsonNode specNode) {
    return specNode != null && ARRAY.equals(specNode.path(CONTAINER).asText());
  }

  static boolean isEnabled(JsonNode feedMeta) {
    return feedMeta != null && feedMeta.isObject() && feedMeta.path("enabled").asBoolean(false);
  }

  // 배열 items는 경로를 늘리지 않는다. 피드 경로는 인덱스가 아니라 스키마 기준이다.
  static Set<String> collect(JsonNode rootSchema) {
    Set<String> paths = new HashSet<>();
    collect(rootSchema, "", paths);
    return paths;
  }

  private static void collect(JsonNode schema, String path, Set<String> paths) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    if (isEnabled(schema.get(FEED_META))) {
      paths.add(path);
      return;
    }
    JsonNode properties = schema.get(PROPERTIES);
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(entry -> collect(entry.getValue(), join(path, entry.getKey()), paths));
    }
    JsonNode items = schema.get(ITEMS);
    if (items != null) {
      collect(items, path, paths);
    }
  }

  // 조상·자손 관계면 교차로 본다. 해당 경로가 피드 필드를 채우는 데 관여하는지 판단하는 기준이다.
  static boolean intersectsFeed(Set<String> feedPaths, String targetPath) {
    String target = normalize(targetPath);
    return feedPaths.stream()
        .map(CurationFeedPaths::normalize)
        .anyMatch(feedPath -> intersects(feedPath, target));
  }

  private static boolean intersects(String feedPath, String targetPath) {
    if (feedPath.isBlank() || targetPath.isBlank()) {
      return true;
    }
    return targetPath.equals(feedPath)
        || targetPath.startsWith(feedPath + ".")
        || feedPath.startsWith(targetPath + ".");
  }

  static String normalize(String path) {
    if (path == null || JSON_PATH_ROOT.equals(path)) {
      return "";
    }
    return path.startsWith(JSON_PATH_PREFIX) ? path.substring(2) : path;
  }

  static String join(String prefix, String suffix) {
    if (prefix == null || prefix.isBlank()) {
      return suffix == null ? "" : suffix;
    }
    if (suffix == null || suffix.isBlank()) {
      return prefix;
    }
    return prefix + "." + suffix;
  }
}
