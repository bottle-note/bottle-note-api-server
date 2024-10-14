package app.bottlenote.support.help.service;

import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpImage;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailInfo;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_AUTHORIZED;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpService {

	private final UserDomainSupport userDomainSupport;
	private final HelpImageSupport helpImageSupport;
	private final HelpRepository helpRepository;

	@Transactional
	public HelpResultResponse registerHelp(HelpUpsertRequest helpUpsertRequest, Long currentUserId) {

		userDomainSupport.isValidUserId(currentUserId);

		Help help = Help.create(
			currentUserId,
			helpUpsertRequest.type(),
			helpUpsertRequest.content());

		List<HelpImage> helpImages = helpUpsertRequest.imageUrlList().stream()
			.map(image -> HelpImage.builder()
				.order(image.order())
				.imageUrl(image.viewUrl())
				.imagePath(ImageUtil.getImagePath(image.viewUrl()))
				.imageKey(ImageUtil.getImageKey(image.viewUrl()))
				.imageName(ImageUtil.getImageName(image.viewUrl()))
				.help(help)
				.build()
			).toList();

		helpImageSupport.saveHelpImages(help, helpImages);

		Help saveHelp = helpRepository.save(help);

		return HelpResultResponse.response(
			REGISTER_SUCCESS,
			saveHelp.getId());
	}

	@Transactional
	public HelpResultResponse modifyHelp(
		HelpUpsertRequest helpUpsertRequest,
		Long currentUserId,
		Long helpId) {

		Help help = helpRepository.findById(helpId)
			.orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

		if (!help.isMyHelpPost(currentUserId)){
			throw new HelpException(HELP_NOT_AUTHORIZED);
		}

		List<HelpImage> helpImages = helpUpsertRequest.imageUrlList().stream()
			.map(image -> HelpImage.builder()
				.order(image.order())
				.imageUrl(image.viewUrl())
				.imagePath(ImageUtil.getImagePath(image.viewUrl()))
				.imageKey(ImageUtil.getImageKey(image.viewUrl()))
				.imageName(ImageUtil.getImageName(image.viewUrl()))
				.help(help)
				.build()
			).toList();

		helpImageSupport.updateHelpImages(help, helpImages);

		help.updateHelp(
			helpUpsertRequest.content(),
			helpUpsertRequest.type());

		return HelpResultResponse.response(
			MODIFY_SUCCESS,
			help.getId());
	}

	@Transactional
	public HelpResultResponse deleteHelp(Long helpId, Long currentUserId) {

		Help help = helpRepository.findById(helpId)
			.orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

		if (!help.isMyHelpPost(currentUserId)){
			throw new HelpException(HELP_NOT_AUTHORIZED);
		}

		help.deleteHelp();

		return HelpResultResponse.response(
			DELETE_SUCCESS,
			help.getId());
	}

	@Transactional(readOnly = true)
	public PageResponse<HelpListResponse> getHelpList(HelpPageableRequest helpPageableRequest, Long currentUserId) {
		return helpRepository.getHelpList(helpPageableRequest, currentUserId);
	}

	@Transactional(readOnly = true)
	public HelpDetailInfo getDetailHelp(Long helpId, Long currentUserId) {

		Help help = helpRepository.findByIdAndUserId(helpId, currentUserId)
			.orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

		return HelpDetailInfo.builder()
			.helpId(help.getId())
			.content(help.getContent())
			.helpType(help.getType())
			.createAt(help.getCreateAt())
			.adminId(help.getAdminId())
			.statusType(help.getStatus())
			.responseContent(help.getResponseContent())
			.lastModifyAt(help.getLastModifyAt())
			.build();
	}
}
