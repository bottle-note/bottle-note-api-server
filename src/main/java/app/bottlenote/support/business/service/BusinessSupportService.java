package app.bottlenote.support.business.service;


import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportListResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.user.facade.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BusinessSupportService {

	private final BusinessSupportRepository repository;
	private final UserFacade userFacade;
	private final ProfanityClient profanityClient;

	private String filter(String content) {
		if (content == null || content.isBlank()) {
			return "";
		}
		return profanityClient.getFilteredText(content);
	}

	@Transactional
	public BusinessSupportResultResponse register(BusinessSupportUpsertRequest req, Long userId) {
		userFacade.isValidUserId(userId);
		String filtered = filter(req.content());
		repository.findTopByUserIdAndContentOrderByIdDesc(userId, filtered)
				.ifPresent(bs -> {
					throw new IllegalStateException("duplicate");
				});
		BusinessSupport bs = BusinessSupport.create(userId, filtered, req.contactWay());
		BusinessSupport saved = repository.save(bs);
		return BusinessSupportResultResponse.response(REGISTER_SUCCESS, saved.getId());
	}

	@Transactional
	public BusinessSupportResultResponse modify(Long id, BusinessSupportUpsertRequest req, Long userId) {
		BusinessSupport bs = repository.findById(id).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));
		if (!bs.isMyPost(userId)) throw new IllegalStateException("unauthorized");
		bs.update(filter(req.content()), req.contactWay());
		return BusinessSupportResultResponse.response(MODIFY_SUCCESS, bs.getId());
	}

	@Transactional
	public BusinessSupportResultResponse delete(Long id, Long userId) {
		BusinessSupport bs = repository.findById(id).orElseThrow();
		if (!bs.isMyPost(userId)) throw new IllegalStateException("unauthorized");
		bs.delete();
		return BusinessSupportResultResponse.response(DELETE_SUCCESS, bs.getId());
	}

	@Transactional(readOnly = true)
	public PageResponse<BusinessSupportListResponse> getList(BusinessSupportPageableRequest req, Long userId) {
		List<BusinessSupport> list = repository.findAllByUserId(userId);
		List<BusinessSupportListResponse.BusinessInfo> infos = list.stream()
				.map(b -> BusinessSupportListResponse.BusinessInfo.of(b.getId(), b.getContent(), b.getCreateAt(), b.getStatus()))
				.toList();
		return PageResponse.of(BusinessSupportListResponse.of((long) infos.size(), infos), CursorPageable.of(list, req.pageSize(), req.cursor()));
	}

	@Transactional(readOnly = true)
	public BusinessSupportDetailItem getDetail(Long id, Long userId) {
		BusinessSupport bs = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));
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
