package app.external.push.service;

import app.external.push.domain.PushStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface PushHandler {

	// 1. 단일 사용자 또는 다중 사용자에게 푸시 전송
	void sendPush(Long userId, String message);
	void sendPush(List<String> userIds, String message);

	// 2. 예약된 푸시 메시지 전송
	void schedulePush(Long userId, String message, LocalDateTime scheduledTime);
	void schedulePush(List<String> userIds, String message, LocalDateTime scheduledTime);

	// 3. 푸시 상태 확인
	PushStatus getPushStatus(String pushId);

	// 4. 예약된 푸시 전송 취소
	void cancelScheduledPush(String pushId);

	// 5. 메시지 템플릿 관리
	String getTemplate(String templateId);
	void saveTemplate(String templateId, String templateContent);
	void deleteTemplate(String templateId);

	// 6. 토큰 관리
	void registerToken(Long userId, String token);
	void unregisterToken(Long userId);
}
