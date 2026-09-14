package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.service.AlcoholBulkInputNormalizer.clean;
import static app.bottlenote.alcohols.service.AlcoholBulkInputNormalizer.key;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkCreateResponse.CreatedRow;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssueItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkRowItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkValidateResponse;
import app.bottlenote.alcohols.dto.response.AlcoholBulkCategoryItem;
import app.bottlenote.alcohols.dto.response.AlcoholBulkReferenceItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.image.ImageUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminAlcoholBulkService implements AdminAlcoholBulkService {
  private static final int MAX_TAGS = 1000;
  private final AlcoholQueryRepository alcoholRepository;
  private final RegionRepository regionRepository;
  private final DistilleryRepository distilleryRepository;
  private final TastingTagRepository tagRepository;
  private final AlcoholsTastingTagsRepository mappingRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional(readOnly = true)
  public AdminAlcoholBulkValidateResponse validate(AdminAlcoholBulkRequest request) {
    checkRequest(request);
    return validateRows(request.rows(), loadReferences(request.rows()));
  }

  @Override
  @Transactional
  public AdminAlcoholBulkCreateResponse create(AdminAlcoholBulkRequest request) {
    checkRequest(request);
    References references = loadReferences(request.rows());
    AdminAlcoholBulkValidateResponse validation = validateRows(request.rows(), references);
    if (validation.invalidRows() > 0) {
      return new AdminAlcoholBulkCreateResponse(0, List.of(), validation);
    }
    List<CreatedRow> created = new ArrayList<>();
    for (AdminAlcoholBulkRowItem result : validation.rows()) {
      AdminAlcoholBulkRowRequest row = result.normalized();
      Alcohol saved =
          alcoholRepository.save(
              Alcohol.builder()
                  .korName(row.korName())
                  .engName(row.engName())
                  .abv(row.abv())
                  .type(AlcoholType.valueOf(row.type()))
                  .korCategory(row.korCategory())
                  .engCategory(row.engCategory())
                  .categoryGroup(AlcoholCategoryGroup.valueOf(row.categoryGroup()))
                  .region(references.regions().get(row.regionId()))
                  .distillery(references.distilleries().get(row.distilleryId()))
                  .age(row.age())
                  .cask(row.cask())
                  .description(row.description())
                  .volume(row.volume())
                  .imageUrl(row.imageUrl())
                  .build());
      if (!row.tastingTagIds().isEmpty()) {
        mappingRepository.saveAll(
            row.tastingTagIds().stream()
                .map(id -> AlcoholsTastingTags.of(saved, references.tags().get(id)))
                .toList());
      }
      if (row.imageUrl() != null) {
        String resourceKey = ImageUtil.extractResourceKey(row.imageUrl());
        if (resourceKey != null) {
          eventPublisher.publishEvent(
              ImageResourceActivatedEvent.of(resourceKey, saved.getId(), "ALCOHOL"));
        }
      }
      created.add(new CreatedRow(row.clientRowId(), saved.getId()));
    }
    return new AdminAlcoholBulkCreateResponse(created.size(), List.copyOf(created), validation);
  }

  private void checkRequest(AdminAlcoholBulkRequest request) {
    if (request == null || request.rows() == null || request.rows().isEmpty()) {
      throw new AlcoholException(AlcoholExceptionCode.BULK_EMPTY_REQUEST);
    }
    if (request.rows().size() > AdminAlcoholBulkRequest.MAX_ROWS) {
      throw new AlcoholException(AlcoholExceptionCode.BULK_ROW_LIMIT_EXCEEDED);
    }
  }

  private References loadReferences(List<AdminAlcoholBulkRowRequest> rows) {
    Set<Long> regionIds = new HashSet<>();
    Set<Long> distilleryIds = new HashSet<>();
    for (AdminAlcoholBulkRowRequest row : rows) {
      if (row == null) continue;
      if (row.regionId() != null && row.regionId() > 0) regionIds.add(row.regionId());
      if (row.distilleryId() != null && row.distilleryId() > 0)
        distilleryIds.add(row.distilleryId());
    }
    Map<Long, Region> regions =
        regionIds.isEmpty()
            ? Map.of()
            : index(regionRepository.findAllByIdInOrderBySortOrderAsc(regionIds), Region::getId);
    Map<Long, Distillery> distilleries =
        distilleryIds.isEmpty()
            ? Map.of()
            : index(
                distilleryRepository.findAllByIdInOrderBySortOrderAsc(distilleryIds),
                Distillery::getId);
    Map<CategoryKey, Set<AlcoholCategoryGroup>> categories = new HashMap<>();
    Map<CategoryKey, Set<AlcoholType>> categoryTypes = new HashMap<>();
    Map<IdentityKey, Set<Long>> candidates = new HashMap<>();
    for (AlcoholBulkCategoryItem item : alcoholRepository.findBulkCategoryItems()) {
      if (item.categoryGroup() != null) {
        categories
            .computeIfAbsent(
                categoryKey(item.korCategory(), item.engCategory()), ignored -> new HashSet<>())
            .add(item.categoryGroup());
      }
      if (item.type() != null) {
        categoryTypes
            .computeIfAbsent(
                categoryKey(item.korCategory(), item.engCategory()), ignored -> new HashSet<>())
            .add(item.type());
      }
    }
    List<AlcoholBulkReferenceItem> existing =
        distilleryIds.isEmpty()
            ? List.of()
            : alcoholRepository.findBulkReferenceItemsByDistilleryIds(List.copyOf(distilleryIds));
    for (AlcoholBulkReferenceItem item : existing) {
      addCandidate(
          candidates,
          identity(item.korName(), item.distilleryId(), item.abv(), item.volume()),
          item.alcoholId());
    }
    return new References(
        regions,
        distilleries,
        index(tagRepository.findAll(), TastingTag::getId),
        categories,
        categoryTypes,
        candidates);
  }

  private AdminAlcoholBulkValidateResponse validateRows(
      List<AdminAlcoholBulkRowRequest> rows, References refs) {
    Map<String, Integer> clientIds = new HashMap<>();
    Map<IdentityKey, Integer> identities = new HashMap<>();
    for (AdminAlcoholBulkRowRequest row : rows) {
      if (row == null) continue;
      if (clean(row.clientRowId()) != null)
        clientIds.merge(clean(row.clientRowId()), 1, Integer::sum);
      identities.merge(
          identity(row.korName(), row.distilleryId(), row.abv(), row.volume()), 1, Integer::sum);
    }
    List<AdminAlcoholBulkRowItem> results = new ArrayList<>();
    for (AdminAlcoholBulkRowRequest row : rows) {
      results.add(validateRow(row, refs, clientIds, identities));
    }
    int valid = (int) results.stream().filter(AdminAlcoholBulkRowItem::valid).count();
    int warning = (int) results.stream().filter(row -> !row.warnings().isEmpty()).count();
    return new AdminAlcoholBulkValidateResponse(
        rows.size(), valid, rows.size() - valid, warning, List.copyOf(results));
  }

  private AdminAlcoholBulkRowItem validateRow(
      AdminAlcoholBulkRowRequest row,
      References refs,
      Map<String, Integer> clientIds,
      Map<IdentityKey, Integer> identities) {
    List<AdminAlcoholBulkIssueItem> errors = new ArrayList<>();
    List<AdminAlcoholBulkIssueItem> warnings = new ArrayList<>();
    if (row == null) {
      return new AdminAlcoholBulkRowItem(
          null,
          false,
          null,
          List.of(issue("REQUIRED", "row", "행은 null일 수 없습니다.")),
          List.of(),
          List.of());
    }
    String clientId = text(row.clientRowId(), "clientRowId", true, errors);
    String korName = text(row.korName(), "korName", true, errors);
    String engName = text(row.engName(), "engName", true, errors);
    String korCategory = text(row.korCategory(), "korCategory", true, errors);
    String engCategory = text(row.engCategory(), "engCategory", true, errors);
    String age = text(row.age(), "age", false, errors);
    String cask = text(row.cask(), "cask", false, errors);
    String image = text(row.imageUrl(), "imageUrl", false, errors);
    String description = clean(row.description());
    if (description != null
        && (description.length() > 65535
            || description.getBytes(StandardCharsets.UTF_8).length > 65535)) {
      errors.add(issue("TOO_LONG", "description", "설명은 UTF-8 기준 65,535바이트를 초과할 수 없습니다."));
    }
    String abv = AlcoholBulkInputNormalizer.quantity(row.abv(), "abv", errors, warnings);
    String volume = AlcoholBulkInputNormalizer.quantity(row.volume(), "volume", errors, warnings);
    AlcoholType type = parseType(clean(row.type()));
    if (type == null) errors.add(issue("INVALID_ENUM", "type", "유효한 주류 타입을 입력해 주세요."));
    AlcoholCategoryGroup group = resolveGroup(row, type, refs.categories(), errors, warnings);
    Set<AlcoholType> categoryTypes =
        refs.categoryTypes().getOrDefault(categoryKey(korCategory, engCategory), Set.of());
    if (type != null && !categoryTypes.isEmpty() && !categoryTypes.contains(type)) {
      warnings.add(issue("TYPE_CATEGORY_MISMATCH", "type", "기존 카테고리의 주류 타입과 다릅니다."));
    }
    checkReference(row.regionId(), "regionId", refs.regions(), errors);
    checkReference(row.distilleryId(), "distilleryId", refs.distilleries(), errors);
    List<Long> tags = tags(row.tastingTagIds(), refs.tags(), errors, warnings);
    if (image != null && !validUrl(image))
      errors.add(issue("INVALID_URL", "imageUrl", "유효한 http 또는 https 이미지 URL을 입력해 주세요."));
    if (clientId != null && clientIds.getOrDefault(clientId, 0) > 1) {
      errors.add(issue("DUPLICATE_CLIENT_ROW_ID", "clientRowId", "요청 내 clientRowId는 유일해야 합니다."));
    }
    IdentityKey identity = identity(korName, row.distilleryId(), row.abv(), row.volume());
    if (identities.getOrDefault(identity, 0) > 1) {
      warnings.add(
          issue("DUPLICATE_REQUEST_ROW", "korName", "요청 안에 이름·증류소·도수·용량이 같은 중복 후보가 있습니다."));
    }
    Set<Long> candidates = refs.candidates().getOrDefault(identity, Set.of());
    if (!candidates.isEmpty())
      warnings.add(issue("DUPLICATE_DB_CANDIDATE", "korName", "DB에 이름·증류소·도수·용량이 같은 중복 후보가 있습니다."));
    if (candidates.size() > 100)
      warnings.add(issue("CANDIDATES_TRUNCATED", "korName", "중복 후보가 많아 ID를 최대 100개까지 반환합니다."));
    AdminAlcoholBulkRowRequest normalized =
        errors.isEmpty()
            ? new AdminAlcoholBulkRowRequest(
                clientId,
                korName,
                engName,
                abv,
                type.name(),
                korCategory,
                engCategory,
                group.name(),
                row.regionId(),
                row.distilleryId(),
                age,
                cask,
                description,
                volume,
                tags,
                image)
            : null;
    return new AdminAlcoholBulkRowItem(
        clientId,
        errors.isEmpty(),
        normalized,
        List.copyOf(errors),
        List.copyOf(warnings),
        candidates.stream().sorted().limit(100).toList());
  }

  private AlcoholCategoryGroup resolveGroup(
      AdminAlcoholBulkRowRequest row,
      AlcoholType type,
      Map<CategoryKey, Set<AlcoholCategoryGroup>> categories,
      List<AdminAlcoholBulkIssueItem> errors,
      List<AdminAlcoholBulkIssueItem> warnings) {
    Set<AlcoholCategoryGroup> known =
        categories.getOrDefault(categoryKey(row.korCategory(), row.engCategory()), Set.of());
    String raw = clean(row.categoryGroup());
    AlcoholCategoryGroup group = null;
    if (raw != null) {
      group =
          Arrays.stream(AlcoholCategoryGroup.values())
              .filter(
                  value -> value.name().equalsIgnoreCase(raw) || value.getDescription().equals(raw))
              .findFirst()
              .orElse(null);
      if (group == null)
        errors.add(issue("INVALID_ENUM", "categoryGroup", "유효한 카테고리 그룹을 입력해 주세요."));
    } else if (known.size() == 1) {
      group = known.iterator().next();
    } else if (type != null && type != AlcoholType.WHISKY) {
      group = AlcoholCategoryGroup.OTHER;
    } else {
      errors.add(
          issue("CATEGORY_GROUP_REQUIRED", "categoryGroup", "카테고리 그룹을 유일하게 추론할 수 없어 직접 입력해야 합니다."));
    }
    if (known.isEmpty())
      warnings.add(issue("UNKNOWN_CATEGORY", "engCategory", "기존 참조에 없는 카테고리 조합을 보존합니다."));
    if (group != null && !known.isEmpty() && !known.contains(group)) {
      warnings.add(issue("CATEGORY_GROUP_MISMATCH", "categoryGroup", "기존 카테고리와 그룹이 다릅니다."));
    }
    if (group != null
        && type != null
        && type != AlcoholType.WHISKY
        && group != AlcoholCategoryGroup.OTHER) {
      warnings.add(
          issue("TYPE_GROUP_MISMATCH", "categoryGroup", "주류 타입과 위스키 카테고리 그룹의 의미를 확인해 주세요."));
    }
    return group;
  }

  private List<Long> tags(
      List<Long> input,
      Map<Long, TastingTag> known,
      List<AdminAlcoholBulkIssueItem> errors,
      List<AdminAlcoholBulkIssueItem> warnings) {
    if (input == null) return List.of();
    if (input.size() > MAX_TAGS) {
      errors.add(issue("TOO_MANY_TAGS", "tastingTagIds", "태그는 행마다 최대 1,000개까지 입력할 수 있습니다."));
      return List.of();
    }
    Set<Long> unique = new LinkedHashSet<>();
    boolean invalid = false;
    boolean duplicate = false;
    for (Long id : input) {
      if (id == null || id <= 0 || !known.containsKey(id)) invalid = true;
      else if (!unique.add(id)) duplicate = true;
    }
    if (invalid)
      errors.add(issue("INVALID_REFERENCE", "tastingTagIds", "null 또는 존재하지 않는 태그 ID가 있습니다."));
    if (duplicate)
      warnings.add(issue("DUPLICATE_TAG_REMOVED", "tastingTagIds", "중복 태그 ID를 제거했습니다."));
    return List.copyOf(unique);
  }

  private static AlcoholType parseType(String raw) {
    if (raw == null) return null;
    return Arrays.stream(AlcoholType.values())
        .filter(
            value ->
                value.name().equalsIgnoreCase(raw)
                    || value.getType().equals(raw)
                    || value.getEngCategory().equalsIgnoreCase(raw))
        .findFirst()
        .orElse(null);
  }

  private static String text(
      String raw, String field, boolean required, List<AdminAlcoholBulkIssueItem> errors) {
    String value = clean(raw);
    if (required && value == null) errors.add(issue("REQUIRED", field, "필수 입력값입니다."));
    if (value != null && value.length() > 255)
      errors.add(issue("TOO_LONG", field, "255자를 초과할 수 없습니다."));
    return value;
  }

  private static boolean validUrl(String value) {
    try {
      URI uri = new URI(value);
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null
          && uri.getRawUserInfo() == null
          && !signedQuery(uri.getRawQuery())
          && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535);
    } catch (URISyntaxException exception) {
      return false;
    }
  }

  private static boolean signedQuery(String rawQuery) {
    if (rawQuery == null) return false;
    String query = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    return Arrays.stream(query.split("&"))
        .map(part -> part.split("=", 2)[0])
        .anyMatch(
            name ->
                name.equals("x-amz-signature")
                    || name.equals("x-goog-signature")
                    || name.equals("signature")
                    || name.equals("awsaccesskeyid"));
  }

  private static void checkReference(
      Long id, String field, Map<Long, ?> known, List<AdminAlcoholBulkIssueItem> errors) {
    if (id == null || id <= 0 || !known.containsKey(id)) {
      errors.add(issue("INVALID_REFERENCE", field, "존재하는 참조 ID를 입력해 주세요."));
    }
  }

  private static <T> Map<Long, T> index(List<T> values, Function<T, Long> id) {
    return values.stream().collect(Collectors.toMap(id, Function.identity()));
  }

  private static IdentityKey identity(String name, Long distilleryId, String abv, String volume) {
    return new IdentityKey(
        key(name), distilleryId, quantityKey(abv, "abv"), quantityKey(volume, "volume"));
  }

  private static String quantityKey(String value, String field) {
    String normalized =
        AlcoholBulkInputNormalizer.quantity(value, field, new ArrayList<>(), new ArrayList<>());
    return key(normalized == null ? value : normalized);
  }

  private static void addCandidate(
      Map<IdentityKey, Set<Long>> index, IdentityKey identity, Long id) {
    if (identity.name().isEmpty() || id == null) return;
    Set<Long> candidates = index.computeIfAbsent(identity, ignored -> new LinkedHashSet<>());
    if (candidates.size() <= 100) candidates.add(id);
  }

  private static CategoryKey categoryKey(String korCategory, String engCategory) {
    return new CategoryKey(key(korCategory), key(engCategory));
  }

  private static AdminAlcoholBulkIssueItem issue(String code, String field, String message) {
    return new AdminAlcoholBulkIssueItem(code, field, message);
  }

  private record IdentityKey(String name, Long distilleryId, String abv, String volume) {}

  private record CategoryKey(String korCategory, String engCategory) {}

  private record References(
      Map<Long, Region> regions,
      Map<Long, Distillery> distilleries,
      Map<Long, TastingTag> tags,
      Map<CategoryKey, Set<AlcoholCategoryGroup>> categories,
      Map<CategoryKey, Set<AlcoholType>> categoryTypes,
      Map<IdentityKey, Set<Long>> candidates) {}
}
