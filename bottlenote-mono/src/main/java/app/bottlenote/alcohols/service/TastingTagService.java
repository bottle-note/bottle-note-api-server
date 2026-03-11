package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_HAS_ALCOHOLS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_HAS_CHILDREN;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_MAX_DEPTH_EXCEEDED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_PARENT_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_ALCOHOL_ADDED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_ALCOHOL_REMOVED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_UPDATED;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.request.AdminTastingTagUpsertRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagDetailResponse;
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TastingTagService {

  private static final int MAX_DEPTH = 3;

  private final TastingTagRepository tastingTagRepository;
  private final AlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  private volatile Trie trie;

  @Transactional(readOnly = true)
  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(cron = "0 0 0 * * *")
  public void initializeTrie() {
    List<TastingTag> tags = tastingTagRepository.findAll();

    Trie.TrieBuilder builder = Trie.builder().ignoreCase();
    for (TastingTag tag : tags) {
      builder.addKeyword(tag.getKorName());
      builder.addKeyword(tag.getEngName());
    }

    this.trie = builder.build();
    log.info("TastingTag Trie 초기화 완료: {}개 태그 등록", tags.size());
  }

  @Transactional(readOnly = true)
  public List<String> extractTagNames(String text) {
    if (trie == null || text == null || text.isBlank()) {
      return List.of();
    }

    return trie.parseText(text).stream().map(Emit::getKeyword).distinct().toList();
  }

  @Transactional(readOnly = true)
  public AdminTastingTagDetailResponse getTagDetail(Long tagId) {
    TastingTag tag =
        tastingTagRepository
            .findById(tagId)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND));

    TastingTagNodeItem parentChain = buildParentChain(tag.getParentId());
    List<TastingTagNodeItem> childrenTree = buildChildrenTree(tagId);

    TastingTagNodeItem tagNode =
        TastingTagNodeItem.of(
            tag.getId(),
            tag.getKorName(),
            tag.getEngName(),
            tag.getIcon(),
            tag.getDescription(),
            parentChain,
            childrenTree);

    List<AdminAlcoholItem> alcohols =
        alcoholsTastingTagsRepository.findByTastingTagId(tagId).stream()
            .map(att -> toAdminAlcoholItem(att.getAlcohol()))
            .toList();

    return AdminTastingTagDetailResponse.of(tagNode, alcohols);
  }

  @Transactional
  public AdminResultResponse createTag(AdminTastingTagUpsertRequest request) {
    if (tastingTagRepository.findByKorName(request.korName()).isPresent()) {
      throw new AlcoholException(TASTING_TAG_DUPLICATE_NAME);
    }

    validateParentAndDepth(request.parentId());

    TastingTag tag =
        TastingTag.builder()
            .korName(request.korName())
            .engName(request.engName())
            .icon(request.icon())
            .description(request.description())
            .parentId(request.parentId())
            .build();

    TastingTag saved = tastingTagRepository.save(tag);
    return AdminResultResponse.of(TASTING_TAG_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse updateTag(Long tagId, AdminTastingTagUpsertRequest request) {
    TastingTag tag =
        tastingTagRepository
            .findById(tagId)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND));

    if (tastingTagRepository.existsByKorNameAndIdNot(request.korName(), tagId)) {
      throw new AlcoholException(TASTING_TAG_DUPLICATE_NAME);
    }

    if (request.parentId() != null && !request.parentId().equals(tag.getParentId())) {
      validateParentAndDepth(request.parentId());
    }

    tag.update(
        request.korName(),
        request.engName(),
        request.icon(),
        request.description(),
        request.parentId());

    return AdminResultResponse.of(TASTING_TAG_UPDATED, tagId);
  }

  @Transactional
  public AdminResultResponse deleteTag(Long tagId) {
    TastingTag tag =
        tastingTagRepository
            .findById(tagId)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND));

    if (tastingTagRepository.existsByParentId(tagId)) {
      throw new AlcoholException(TASTING_TAG_HAS_CHILDREN);
    }

    if (alcoholsTastingTagsRepository.existsByTastingTagId(tagId)) {
      throw new AlcoholException(TASTING_TAG_HAS_ALCOHOLS);
    }

    tastingTagRepository.delete(tag);
    return AdminResultResponse.of(TASTING_TAG_DELETED, tagId);
  }

  @Transactional
  public AdminResultResponse addAlcoholsToTag(Long tagId, List<Long> alcoholIds) {
    TastingTag tag =
        tastingTagRepository
            .findById(tagId)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND));

    List<AlcoholsTastingTags> newMappings = new ArrayList<>();
    for (Long alcoholId : alcoholIds) {
      Alcohol alcohol =
          alcoholQueryRepository
              .findById(alcoholId)
              .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));
      newMappings.add(AlcoholsTastingTags.of(alcohol, tag));
    }

    alcoholsTastingTagsRepository.saveAll(newMappings);
    return AdminResultResponse.of(TASTING_TAG_ALCOHOL_ADDED, tagId);
  }

  @Transactional
  public AdminResultResponse removeAlcoholsFromTag(Long tagId, List<Long> alcoholIds) {
    if (!tastingTagRepository.findById(tagId).isPresent()) {
      throw new AlcoholException(TASTING_TAG_NOT_FOUND);
    }

    alcoholsTastingTagsRepository.deleteByTastingTagIdAndAlcoholIdIn(tagId, alcoholIds);
    return AdminResultResponse.of(TASTING_TAG_ALCOHOL_REMOVED, tagId);
  }

  /** 부모 체인을 마트료시카 구조로 빌드 (parent.parent.parent...) */
  private TastingTagNodeItem buildParentChain(Long parentId) {
    if (parentId == null) {
      return null;
    }

    TastingTag parent = tastingTagRepository.findById(parentId).orElse(null);
    if (parent == null) {
      return null;
    }

    return TastingTagNodeItem.of(
        parent.getId(),
        parent.getKorName(),
        parent.getEngName(),
        parent.getIcon(),
        parent.getDescription(),
        buildParentChain(parent.getParentId()),
        null);
  }

  /** 자식 트리를 마트료시카 구조로 빌드 (children[].children[]...) */
  private List<TastingTagNodeItem> buildChildrenTree(Long tagId) {
    List<TastingTag> children = tastingTagRepository.findByParentId(tagId);
    if (children.isEmpty()) {
      return List.of();
    }

    return children.stream()
        .map(
            child ->
                TastingTagNodeItem.of(
                    child.getId(),
                    child.getKorName(),
                    child.getEngName(),
                    child.getIcon(),
                    child.getDescription(),
                    null,
                    buildChildrenTree(child.getId())))
        .toList();
  }

  private void validateParentAndDepth(Long parentId) {
    if (parentId == null) return;

    TastingTag parent =
        tastingTagRepository
            .findById(parentId)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_PARENT_NOT_FOUND));

    int parentDepth = calculateDepth(parent);
    if (parentDepth >= MAX_DEPTH) {
      throw new AlcoholException(TASTING_TAG_MAX_DEPTH_EXCEEDED);
    }
  }

  private int calculateDepth(TastingTag tag) {
    int depth = 1;
    Long currentParentId = tag.getParentId();

    while (currentParentId != null && depth < MAX_DEPTH) {
      TastingTag parent = tastingTagRepository.findById(currentParentId).orElse(null);
      if (parent == null) break;

      currentParentId = parent.getParentId();
      depth++;
    }

    return depth;
  }

  private AdminAlcoholItem toAdminAlcoholItem(Alcohol alcohol) {
    return new AdminAlcoholItem(
        alcohol.getId(),
        alcohol.getKorName(),
        alcohol.getEngName(),
        alcohol.getKorCategory(),
        alcohol.getEngCategory(),
        alcohol.getImageUrl(),
        alcohol.getCreateAt(),
        alcohol.getLastModifyAt(),
        alcohol.getDeletedAt());
  }
}
