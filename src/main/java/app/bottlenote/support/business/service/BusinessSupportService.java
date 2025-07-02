package app.bottlenote.support.business.service;


import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.exception.BusinessSupportException;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import app.bottlenote.user.facade.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_DUPLICATE;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_AUTHORIZED;
import static app.bottlenote.support.business.exception.BusinessSupportExceptionCode.BUSINESS_SUPPORT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BusinessSupportService {

	private final BusinessSupportRepository repository;
	private final UserFacade userFacade;
	private final ProfanityClient profanityClient;

	@Transactional
	public BusinessSupportResultResponse register(BusinessSupportUpsertRequest req, Long userId) {
		userFacade.isValidUserId(userId);
		String filtered = profanityClient.filter(req.content());
		repository.findTopByUserIdAndContentOrderByIdDesc(userId, filtered)
				.ifPresent(bs -> {
					throw new BusinessSupportException(BUSINESS_SUPPORT_DUPLICATE);
				});
		BusinessSupport bs = BusinessSupport.create(userId, filtered, req.contactWay());
		BusinessSupport saved = repository.save(bs);
		return BusinessSupportResultResponse.response(REGISTER_SUCCESS, saved.getId());
	}

	@Transactional
	public BusinessSupportResultResponse modify(Long id, BusinessSupportUpsertRequest req, Long userId) {
		BusinessSupport bs = repository.findById(id).orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
		if (!bs.isMyPost(userId)) throw new BusinessSupportException(BUSINESS_SUPPORT_NOT_AUTHORIZED);
		bs.update(profanityClient.filter(req.content()), req.contactWay());
		return BusinessSupportResultResponse.response(MODIFY_SUCCESS, bs.getId());
	}

	@Transactional
	public BusinessSupportResultResponse delete(Long id, Long userId) {
		BusinessSupport bs = repository.findById(id).orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
		if (!bs.isMyPost(userId)) throw new BusinessSupportException(BUSINESS_SUPPORT_NOT_AUTHORIZED);
		bs.delete();
		return BusinessSupportResultResponse.response(DELETE_SUCCESS, bs.getId());
	}

	@Transactional(readOnly = true)
	public CollectionResponse<BusinessInfoResponse> getList(BusinessSupportPageableRequest req, Long userId) {
		List<BusinessSupport> list = repository.findAllByUserId(userId);
		List<BusinessInfoResponse> infos = list.stream()
				.map(b -> BusinessInfoResponse.of(b.getId(), b.getContent(), b.getCreateAt(), b.getStatus()))
				.toList();
		return CollectionResponse.of(infos.size(), infos);
	}

	@Transactional(readOnly = true)
	public BusinessSupportDetailItem getDetail(Long id, Long userId) {
		BusinessSupport bs = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new BusinessSupportException(BUSINESS_SUPPORT_NOT_FOUND));
		return BusinessSupportDetailItem.builder()
				.id(bs.getId())
				.content(bs.getContent())
				.contactWay(bs.getContactWay().name())
				.createAt(bs.getCreateAt())
				.status(bs.getStatus())
				.adminId(bs.getAdminId())
				.responseContent(bs.getResponseContent())
				.lastModifyAt(bs.getLastModifyAt())
				.build();
	}
}
