package app.external.push.service;

import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.service.domain.UserDomainSupport;
import app.external.push.domain.PushStatus;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPushHandler implements PushHandler {

	private static final String TITLE = "Bottle Note";
	private final UserDomainSupport userDomainSupport;

	@Override
	public void sendPush(Long userId, String message) {
		UserProfileInfo userProfileInfo = userDomainSupport.getUserProfileInfo(userId);
		String nickname = userProfileInfo.nickname();
		try {
			String result = FirebaseMessaging.getInstance().send(Message.builder()
				.setNotification(Notification.builder()
					.setTitle(TITLE)
					.setBody(message)
					.build())
				.setToken(nickname)
				.build());
			log.debug("성공적으로 메시지를 보냈습니다: {}", result);
		} catch (FirebaseMessagingException e) {
			log.error("Error sending message: {}", e.getMessage());
		}
	}

	@Override
	public void sendPush(List<String> userIds, String message) {

	}

	@Override
	public void schedulePush(Long userId, String message, LocalDateTime scheduledTime) {

	}

	@Override
	public void schedulePush(List<String> userIds, String message, LocalDateTime scheduledTime) {

	}

	@Override
	public PushStatus getPushStatus(String pushId) {
		return null;
	}

	@Override
	public void cancelScheduledPush(String pushId) {

	}

	@Override
	public String getTemplate(String templateId) {
		return "";
	}

	@Override
	public void saveTemplate(String templateId, String templateContent) {

	}

	@Override
	public void deleteTemplate(String templateId) {

	}

	@Override
	public void registerToken(Long userId, String token) {

	}

	@Override
	public void unregisterToken(Long userId) {

	}
}
