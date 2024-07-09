package app.bottlenote.user.service;

import app.bottlenote.user.dto.request.NotificationMessage;

public interface NotificationService {
	void sendNotification(NotificationMessage message);
}
