package app.external.push.application;

import java.time.LocalDateTime;
import java.util.List;

public interface PushHandler {

  void sendPush(List<Long> userIds, String message);

  void schedulePush(List<Long> userIds, String message, LocalDateTime scheduledTime);
}
