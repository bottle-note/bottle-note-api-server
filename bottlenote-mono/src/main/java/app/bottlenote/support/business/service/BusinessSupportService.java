package app.bottlenote.support.business.service;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_DUPLICATE;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_AUTHORIZED;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_FOUND;

import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.event.payload.ImageResourceInvalidatedEvent;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.domain.BusinessSupportRepository;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportListResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.exception.BusinessSupportException;
import app.bottlenote.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessSupportService {

  private static final String REFERENCE_TYPE_BUSINESS = "BUSINESS";

  private final BusinessSupportRepository repository;
  private final UserFacade userFacade;
  private final ProfanityClient profanityClient;
  private final ApplicationEventPublisher eventPublisher;
  private final HmacCursorCodec cursorCodec;

  @Transactional
  public BusinessSupportResultResponse register(BusinessSupportUpsertRequest req, Long userId) {
    userFacade.isValidUserId(userId);
    String filteredTitle = profanityClient.filter(req.title());
    String filteredContent = profanityClient.filter(req.content());
    repository
        .findTopByUserIdAndContentOrderByIdDesc(userId, filteredContent)
        .ifPresent(
            bs -> {
              throw new BusinessSupportException(BUSINESS_SUPPORT_DUPLICATE);
            });
    BusinessSupport bs =
        BusinessSupport.create(
            userId, filteredTitle, filteredContent, req.contact(), req.businessSupportType());
    BusinessSupport saved = repository.save(bs);

    // 이미지 저장
    bs.saveImages(req.imageUrlList(), saved.getId());

    publishImageActivatedEvent(req.imageUrlList(), saved.getId());

    return BusinessSupportResultResponse.response(REGISTER_SUCCESS, saved.getId());
  }

  @Transactional
  public BusinessSupportResultResponse modify(
      Long id, BusinessSupportUpsertRequest req, Long userId) {
    BusinessSupport bs =
        repository
            .findById(id)
            .orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
    if (!bs.isMyPost(userId)) throw new BusinessSupportException(BUSINESS_SUPPORT_NOT_AUTHORIZED);

    // 기존 이미지 목록 추출 (수정 전)
    List<String> oldImageUrls =
        bs.getBusinessImageList().getBusinessImages().stream()
            .map(image -> image.getBusinessImageInfo().getImageUrl())
            .toList();

    String filteredTitle = profanityClient.filter(req.title());
    String filteredContent = profanityClient.filter(req.content());
    bs.update(
        filteredTitle,
        filteredContent,
        req.contact(),
        req.businessSupportType(),
        req.imageUrlList());

    // 새 이미지 목록 추출
    List<String> newImageUrls =
        Objects.requireNonNullElse(req.imageUrlList(), Collections.<BusinessImageItem>emptyList())
            .stream()
            .map(BusinessImageItem::viewUrl)
            .toList();

    // 제거된 이미지에 대해 INVALIDATED 이벤트 발행
    publishImageInvalidatedEvent(oldImageUrls, newImageUrls, bs.getId());

    // 새 이미지에 대해 ACTIVATED 이벤트 발행
    publishImageActivatedEvent(req.imageUrlList(), bs.getId());

    return BusinessSupportResultResponse.response(MODIFY_SUCCESS, bs.getId());
  }

  @Transactional
  public BusinessSupportResultResponse delete(Long id, Long userId) {
    BusinessSupport bs =
        repository
            .findById(id)
            .orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
    if (!bs.isMyPost(userId)) throw new BusinessSupportException(BUSINESS_SUPPORT_NOT_AUTHORIZED);

    // 기존 이미지 목록 추출 (삭제 전)
    List<String> oldImageUrls =
        bs.getBusinessImageList().getBusinessImages().stream()
            .map(image -> image.getBusinessImageInfo().getImageUrl())
            .toList();

    bs.delete();

    // 모든 이미지에 대해 INVALIDATED 이벤트 발행
    publishImageInvalidatedEvent(oldImageUrls, Collections.emptyList(), bs.getId());

    return BusinessSupportResultResponse.response(DELETE_SUCCESS, bs.getId());
  }

  @Transactional(readOnly = true)
  public PageResponse<BusinessSupportListResponse> getList(
      BusinessSupportPageableRequest req, Long userId) {
    String context = "business-support.list:" + userId;
    List<BusinessSupport> fetched =
        repository.findAllByUserId(userId).stream()
            .sorted(
                Comparator.comparing(
                        BusinessSupport::getCreateAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        BusinessSupport::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .filter(item -> afterCursor(item, req.cursor(), context))
            .limit(req.size() + 1L)
            .toList();
    Pagination.PageSlice<BusinessInfoResponse> slice =
        Pagination.fromOverflow(
            fetched.stream().map(this::toInfo).toList(),
            req.size(),
            item ->
                cursorCodec.encode(
                    context,
                    TimeIdCursor.keys(
                        item.createAt() == null ? LocalDateTime.MIN : item.createAt(), item.id())));
    return PageResponse.of(new BusinessSupportListResponse(slice.items()), slice.pagination());
  }

  private boolean afterCursor(BusinessSupport item, String cursor, String context) {
    if (cursor == null || cursor.isBlank()) {
      return true;
    }
    CursorClaims claims = cursorCodec.verify(cursor, context);
    LocalDateTime lastCreateAt = TimeIdCursor.time(claims);
    Long lastId = TimeIdCursor.id(claims);
    if (item.getCreateAt() == null) {
      return true;
    }
    if (item.getCreateAt().isBefore(lastCreateAt)) {
      return true;
    }
    return item.getCreateAt().isEqual(lastCreateAt) && item.getId() < lastId;
  }

  private BusinessInfoResponse toInfo(BusinessSupport support) {
    return BusinessInfoResponse.of(
        support.getId(),
        support.getTitle(),
        support.getContent(),
        support.getCreateAt(),
        support.getStatus());
  }

  @Transactional(readOnly = true)
  public BusinessSupportDetailItem getDetail(Long id, Long userId) {
    BusinessSupport bs =
        repository
            .findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
    return BusinessSupportDetailItem.builder()
        .id(bs.getId())
        .title(bs.getTitle())
        .content(bs.getContent())
        .contact(bs.getContact())
        .businessSupportType(bs.getBusinessSupportType())
        .imageUrlList(
            bs.getBusinessImageList().getBusinessImages().stream()
                .map(
                    image ->
                        BusinessImageItem.create(
                            image.getBusinessImageInfo().getOrder(),
                            image.getBusinessImageInfo().getImageUrl()))
                .toList())
        .createAt(bs.getCreateAt())
        .status(bs.getStatus())
        .adminId(bs.getAdminId())
        .responseContent(bs.getResponseContent())
        .lastModifyAt(bs.getLastModifyAt())
        .build();
  }

  private void publishImageActivatedEvent(List<BusinessImageItem> imageList, Long businessId) {
    if (imageList == null || imageList.isEmpty() || businessId == null) {
      return;
    }
    List<String> resourceKeys =
        imageList.stream()
            .map(BusinessImageItem::viewUrl)
            .map(ImageUtil::extractResourceKey)
            .filter(Objects::nonNull)
            .toList();
    if (!resourceKeys.isEmpty()) {
      eventPublisher.publishEvent(
          ImageResourceActivatedEvent.of(resourceKeys, businessId, REFERENCE_TYPE_BUSINESS));
    }
  }

  private void publishImageInvalidatedEvent(
      List<String> oldImageUrls, List<String> newImageUrls, Long businessId) {
    if (oldImageUrls == null || oldImageUrls.isEmpty() || businessId == null) {
      return;
    }
    Set<String> newUrlSet =
        new HashSet<>(Objects.requireNonNullElse(newImageUrls, Collections.emptyList()));
    List<String> removedResourceKeys =
        oldImageUrls.stream()
            .filter(url -> !newUrlSet.contains(url))
            .map(ImageUtil::extractResourceKey)
            .filter(Objects::nonNull)
            .toList();
    if (!removedResourceKeys.isEmpty()) {
      eventPublisher.publishEvent(
          ImageResourceInvalidatedEvent.of(
              removedResourceKeys, businessId, REFERENCE_TYPE_BUSINESS));
    }
  }
}
