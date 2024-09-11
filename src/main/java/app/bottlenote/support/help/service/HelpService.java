package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpService {

	private final UserDomainSupport userDomainSupport;
	private final HelpRepository helpRepository;

	@Transactional
	public HelpRegisterResponse registerHelp(HelpUpsertRequest helpUpsertRequest, Long currentUserId) {

		userDomainSupport.isValidUserId(currentUserId);

		Help help = Help.create(
			currentUserId,
			helpUpsertRequest.title(),
			helpUpsertRequest.content(),
			helpUpsertRequest.type()
		);

		Help saveHelp = helpRepository.save(help);

		return HelpRegisterResponse.response(
			REGISTER_SUCCESS,
			saveHelp.getId());
	}
}
