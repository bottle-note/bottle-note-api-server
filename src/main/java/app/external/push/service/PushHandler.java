package app.external.push.service;

import java.time.LocalDateTime;
import java.util.List;

public interface PushHandler {

	// 1. 단일 사용자 또는 다중 사용자에게 푸시 전송
	void sendPush(Long userId, String message);

	void sendPush(List<String> userIds, String message);

	// 2. 예약된 푸시 메시지 전송
	void schedulePush(Long userId, String message, LocalDateTime scheduledTime);

	void schedulePush(List<String> userIds, String message, LocalDateTime scheduledTime);

	// 메시지 템플릿 관리
	// String getTemplate(String templateId);
	// void saveTemplate(String templateId, String templateContent);
	// void deleteTemplate(String templateId);
}
