package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpUpsertResponse;
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
	public HelpUpsertResponse registerHelp(HelpUpsertRequest helpUpsertRequest, Long currentUserId) {

		userDomainSupport.isValidUserId(currentUserId);

		Help help = Help.create(
			currentUserId,
			helpUpsertRequest.title(),
			helpUpsertRequest.content(),
			helpUpsertRequest.type()
		);

		Help saveHelp = helpRepository.save(help);

		return HelpUpsertResponse.response(
			REGISTER_SUCCESS,
			saveHelp.getId());
	}

	@Transactional
	public HelpUpsertResponse modifyHelp(
		HelpUpsertRequest helpUpsertRequest,
		Long currentUserId,
		Long helpId) {

		Help help = helpRepository.findByIdAndUserId(helpId, currentUserId)
			.orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

		help.updateHelp(
			helpUpsertRequest.title(),
			helpUpsertRequest.content(),
			helpUpsertRequest.type());

		return HelpUpsertResponse.response(
			MODIFY_SUCCESS,
			help.getId());
	}
}
