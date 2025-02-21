package app.external.notification.application;

import app.external.notification.data.payload.NotificationMessage;

public interface NotificationService {
	void sendNotification(NotificationMessage message);
}
