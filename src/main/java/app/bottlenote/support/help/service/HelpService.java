package app.bottlenote.support.help.service;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpService {

	private final UserDomainSupport userDomainSupport;
	private final HelpRepository helpRepository;

	@Transactional
	public HelpRegisterResponse registerHelp(HelpRegisterRequest helpRegisterRequest, Long currentUserId) {

		userDomainSupport.isValidUserId(currentUserId);

		Help help = Help.create(
			currentUserId,
			helpRegisterRequest.title(),
			helpRegisterRequest.content(),
			helpRegisterRequest.type()
		);

		Help saveHelp = helpRepository.save(help);

		return HelpRegisterResponse.response(
			REGISTER_SUCCESS,
			saveHelp.getId());
	}
}
