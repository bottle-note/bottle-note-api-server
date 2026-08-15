package app.bottlenote.support.help.service;

import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpRepository;
import app.bottlenote.support.help.dto.request.AdminHelpAnswerRequest;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import app.bottlenote.support.help.dto.response.AdminHelpAnswerResponse;
import app.bottlenote.support.help.dto.response.AdminHelpDetailResponse;
import app.bottlenote.support.help.event.payload.HelpAnswerNotificationEvent;
import app.bottlenote.support.help.exception.HelpException;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminHelpService {

  private final HelpRepository helpRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public GlobalResponse getHelpList(AdminHelpPageableRequest request) {
    return GlobalResponse.fromPage(helpRepository.getAdminHelpList(request));
  }

  @Transactional(readOnly = true)
  public AdminHelpDetailResponse getHelpDetail(Long helpId) {
    Help help =
        helpRepository.findById(helpId).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

    String userNickname =
        userRepository.findById(help.getUserId()).map(User::getNickName).orElse("탈퇴한 사용자");

    return AdminHelpDetailResponse.builder()
        .helpId(help.getId())
        .userId(help.getUserId())
        .userNickname(userNickname)
        .title(help.getTitle())
        .content(help.getContent())
        .type(help.getType())
        .imageUrlList(
            help.getHelpImageList().getHelpImages().stream()
                .map(
                    image ->
                        HelpImageItem.create(
                            image.getHelpimageInfo().getOrder(),
                            image.getHelpimageInfo().getImageUrl()))
                .toList())
        .status(help.getStatus())
        .adminId(help.getAdminId())
        .responseContent(help.getResponseContent())
        .createAt(help.getCreateAt())
        .lastModifyAt(help.getLastModifyAt())
        .build();
  }

  @Transactional
  public AdminHelpAnswerResponse answerHelp(
      Long helpId, Long adminId, AdminHelpAnswerRequest request) {
    Help help =
        helpRepository.findById(helpId).orElseThrow(() -> new HelpException(HELP_NOT_FOUND));

    help.answer(adminId, request.responseContent(), request.status());
    eventPublisher.publishEvent(
        HelpAnswerNotificationEvent.of(help.getId(), help.getUserId(), request.responseContent()));

    return AdminHelpAnswerResponse.of(help.getId(), help.getStatus());
  }
}
