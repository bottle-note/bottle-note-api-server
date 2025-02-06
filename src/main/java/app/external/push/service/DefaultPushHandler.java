package app.external.push.service;

import app.bottlenote.user.service.UserFacade;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPushHandler implements PushHandler {
	private static final String DEFAULT_TITLE = "Bottle Note 에서 새로운 소식이 도착했어요!";
	private final UserFacade tokenService;

	private void push(MulticastMessage multicastMessage) {
		try {
			BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);
			log.debug("전송 {}", response.getSuccessCount());
		} catch (FirebaseMessagingException e) {
			log.error("Error sending message: {}", e.getMessage());
		}
	}

	@Override
	public void sendPush(List<Long> userIds, String message) {
		List<String> tokens = tokenService.getAvailableUserTokens(userIds);
		MulticastMessage multicastMessage = MulticastMessage.builder()
			.setNotification(Notification.builder()
				.setTitle(DEFAULT_TITLE)
				.setBody(message)
				.build())
			.addAllTokens(tokens)
			.build();
		push(multicastMessage);

	}

	@Override
	public void schedulePush(List<Long> userIds, String message, LocalDateTime scheduledTime) {

		List<String> tokens = tokenService.getAvailableUserTokens(userIds);
		MulticastMessage multicastMessage = MulticastMessage.builder()
			.setNotification(Notification.builder()
				.setTitle(DEFAULT_TITLE)
				.setBody(message)
				.build())
			.addAllTokens(tokens)
			.build();

		// Queue에 메시지와 저장시간 발솔 예정 시간 저장
	}

	@Scheduled(fixedRate = 60000)
	public void processPendingPushes() {
		LocalDateTime now = LocalDateTime.now();
		log.info("메시지 발행 처리 시작: {}", now);
	/*	List<ScheduledPush> pendingPushes = scheduledPushRepository
			.findByStatusAndScheduledTimeLessThanEqual("PENDING", now);
		for (ScheduledPush push : pendingPushes) {
			try {
				List<Long> userIds = objectMapper.readValue(push.getUserIds(),
					new TypeReference<List<Long>>() {
					});
				pushHandler.sendPush(userIds, push.getMessage());
				push.setStatus("SENT");
				push.setSentAt(LocalDateTime.now());
			} catch (Exception e) {
				push.setStatus("FAILED");
				log.error("Failed to send scheduled push: {}", e.getMessage());
			}
			scheduledPushRepository.save(push);*/
	}
}
