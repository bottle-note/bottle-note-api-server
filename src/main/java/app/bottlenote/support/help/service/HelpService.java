package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpService {

	private final UserDomainSupport userDomainSupport;
	private final HelpRepository helpRepository;

	@Transactional
	public HelpResultResponse registerHelp(HelpUpsertRequest helpUpsertRequest, Long currentUserId) {

		userDomainSupport.isValidUserId(currentUserId);

		Help help = Help.create(
			currentUserId,
			helpUpsertRequest.title(),
			helpUpsertRequest.content(),
			helpUpsertRequest.type()
		);

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

		Help help = helpRepository.findByIdAndUserId(helpId, currentUserId)
			.orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

		help.updateHelp(
			helpUpsertRequest.title(),
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

		if (!help.getUserId().equals(currentUserId)){
			throw new HelpException(HELP_NOT_AUTHORIZED);
		}

		help.deleteHelp();

		return HelpResultResponse.response(
			DELETE_SUCCESS,
			help.getId());
	}
}
