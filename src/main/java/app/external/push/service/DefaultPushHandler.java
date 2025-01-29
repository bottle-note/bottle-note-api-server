package app.external.push.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
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

	private static final String DEFAULT_TITLE = "Bottle Note";
	private final UserDeviceService userDeviceService;

	@Override
	public void sendPush(Long userId, String message) {
		final String token = userDeviceService.loadUserToken(userId);
		try {

			Message messageContent = Message.builder()
				.setNotification(Notification.builder()
					.setTitle(DEFAULT_TITLE)
					.setBody(message)
					.build())
				.setToken(token)
				.build();

			String result = FirebaseMessaging.getInstance()
				.send(messageContent);
			log.debug("성공적으로 메시지를 보냈습니다: {}", result);
		} catch (FirebaseMessagingException e) {
			log.error("Error sending message: {}", e.getMessage());
		}
	}

	@Override
	public void sendPush(List<String> userIds, String message) {
		List<String> tokens = userDeviceService.loadUserTokens(userIds);
		MulticastMessage multicastMessage = MulticastMessage.builder()
			.setNotification(Notification.builder()
				.setTitle(DEFAULT_TITLE)
				.setBody(message)
				.build())
			.addAllTokens(tokens)
			.build();

		try {
			BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);
			log.debug("멀티 전송 {}", response.getSuccessCount());
		} catch (FirebaseMessagingException e) {
			log.error("Error sending message: {}", e.getMessage());
		}
	}

	@Override
	public void schedulePush(Long userId, String message, LocalDateTime scheduledTime) {

	}

	@Override
	public void schedulePush(List<String> userIds, String message, LocalDateTime scheduledTime) {

	}
}
