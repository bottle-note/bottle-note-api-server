package app.bottlenote.support.help.service;

import static app.bottlenote.support.help.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_AUTHORIZED;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;

import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.event.payload.ImageResourceInvalidatedEvent;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpRepository;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailItem;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.user.facade.UserFacade;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpService {

  private static final String REFERENCE_TYPE_HELP = "HELP";

  private final UserFacade userDomainSupport;
  private final HelpRepository helpRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public HelpResultResponse registerHelp(HelpUpsertRequest helpUpsertRequest, Long currentUserId) {

    userDomainSupport.isValidUserId(currentUserId);

    Help help =
        Help.create(
            currentUserId,
            helpUpsertRequest.type(),
            helpUpsertRequest.title(),
            helpUpsertRequest.content());

    // 문의글 저장
    Help saveHelp = helpRepository.save(help);

    // 문의글 이미지 저장
    help.saveImages(helpUpsertRequest.imageUrlList(), help.getId());

    publishImageActivatedEvent(helpUpsertRequest.imageUrlList(), saveHelp.getId());

    return HelpResultResponse.response(REGISTER_SUCCESS, saveHelp.getId());
  }

  @Transactional
  public HelpResultResponse modifyHelp(
      HelpUpsertRequest helpUpsertRequest, Long currentUserId, Long helpId) {

    Help help =
        helpRepository.findById(helpId).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

    if (!help.isMyHelpPost(currentUserId)) {
      throw new HelpException(HELP_NOT_AUTHORIZED);
    }

    // 기존 이미지 목록 추출 (수정 전)
    List<String> oldImageUrls =
        help.getHelpImageList().getHelpImages().stream()
            .map(image -> image.getHelpimageInfo().getImageUrl())
            .toList();

    help.updateHelp(
        helpUpsertRequest.title(),
        helpUpsertRequest.content(),
        helpUpsertRequest.imageUrlList(),
        helpUpsertRequest.type());

    // 새 이미지 목록 추출
    List<String> newImageUrls =
        Objects.requireNonNullElse(
                helpUpsertRequest.imageUrlList(), Collections.<HelpImageItem>emptyList())
            .stream()
            .map(HelpImageItem::viewUrl)
            .toList();

    // 제거된 이미지에 대해 INVALIDATED 이벤트 발행
    publishImageInvalidatedEvent(oldImageUrls, newImageUrls, help.getId());

    // 새 이미지에 대해 ACTIVATED 이벤트 발행
    publishImageActivatedEvent(helpUpsertRequest.imageUrlList(), help.getId());

    return HelpResultResponse.response(MODIFY_SUCCESS, help.getId());
  }

  @Transactional
  public HelpResultResponse deleteHelp(Long helpId, Long currentUserId) {

    Help help =
        helpRepository.findById(helpId).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

    if (!help.isMyHelpPost(currentUserId)) {
      throw new HelpException(HELP_NOT_AUTHORIZED);
    }

    // 기존 이미지 목록 추출 (삭제 전)
    List<String> oldImageUrls =
        help.getHelpImageList().getHelpImages().stream()
            .map(image -> image.getHelpimageInfo().getImageUrl())
            .toList();

    help.deleteHelp();

    // 모든 이미지에 대해 INVALIDATED 이벤트 발행
    publishImageInvalidatedEvent(oldImageUrls, Collections.emptyList(), help.getId());

    return HelpResultResponse.response(DELETE_SUCCESS, help.getId());
  }

  @Transactional(readOnly = true)
  public PageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId) {
    return helpRepository.getHelpList(helpPageableRequest, currentUserId);
  }

  @Transactional(readOnly = true)
  public HelpDetailItem getDetailHelp(Long helpId, Long currentUserId) {

    Help help =
        helpRepository
            .findByIdAndUserId(helpId, currentUserId)
            .orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

    return HelpDetailItem.builder()
        .helpId(help.getId())
        .title(help.getTitle())
        .content(help.getContent())
        .helpType(help.getType())
        .imageUrlList(
            help.getHelpImageList().getHelpImages().stream()
                .map(
                    image ->
                        HelpImageItem.create(
                            image.getHelpimageInfo().getOrder(),
                            image.getHelpimageInfo().getImageUrl()))
                .toList())
        .createAt(help.getCreateAt())
        .adminId(help.getAdminId())
        .statusType(help.getStatus())
        .responseContent(help.getResponseContent())
        .lastModifyAt(help.getLastModifyAt())
        .build();
  }

  private void publishImageActivatedEvent(List<HelpImageItem> imageList, Long helpId) {
    if (imageList == null || imageList.isEmpty() || helpId == null) {
      return;
    }
    List<String> resourceKeys =
        imageList.stream()
            .map(HelpImageItem::viewUrl)
            .map(ImageUtil::extractResourceKey)
            .filter(Objects::nonNull)
            .toList();
    if (!resourceKeys.isEmpty()) {
      eventPublisher.publishEvent(
          ImageResourceActivatedEvent.of(resourceKeys, helpId, REFERENCE_TYPE_HELP));
    }
  }

  private void publishImageInvalidatedEvent(
      List<String> oldImageUrls, List<String> newImageUrls, Long helpId) {
    if (oldImageUrls == null || oldImageUrls.isEmpty() || helpId == null) {
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
          ImageResourceInvalidatedEvent.of(removedResourceKeys, helpId, REFERENCE_TYPE_HELP));
    }
  }
}
