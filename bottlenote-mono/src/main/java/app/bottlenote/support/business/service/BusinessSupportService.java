package app.bottlenote.support.business.service;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_DUPLICATE;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_AUTHORIZED;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_FOUND;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.domain.BusinessSupportRepository;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.exception.BusinessSupportException;
import app.bottlenote.user.facade.UserFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessSupportService {

  private final BusinessSupportRepository repository;
  private final UserFacade userFacade;
  private final ProfanityClient profanityClient;

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
    String filteredTitle = profanityClient.filter(req.title());
    String filteredContent = profanityClient.filter(req.content());
    bs.update(
        filteredTitle,
        filteredContent,
        req.contact(),
        req.businessSupportType(),
        req.imageUrlList());
    return BusinessSupportResultResponse.response(MODIFY_SUCCESS, bs.getId());
  }

  @Transactional
  public BusinessSupportResultResponse delete(Long id, Long userId) {
    BusinessSupport bs =
        repository
            .findById(id)
            .orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
    if (!bs.isMyPost(userId)) throw new BusinessSupportException(BUSINESS_SUPPORT_NOT_AUTHORIZED);
    bs.delete();
    return BusinessSupportResultResponse.response(DELETE_SUCCESS, bs.getId());
  }

  @Transactional(readOnly = true)
  public CollectionResponse<BusinessInfoResponse> getList(
      BusinessSupportPageableRequest req, Long userId) {
    List<BusinessSupport> list = repository.findAllByUserId(userId);
    List<BusinessInfoResponse> infos =
        list.stream()
            .map(
                b ->
                    BusinessInfoResponse.of(
                        b.getId(), b.getTitle(), b.getContent(), b.getCreateAt(), b.getStatus()))
            .toList();
    return CollectionResponse.of(infos.size(), infos);
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
}
