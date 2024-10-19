package app.bottlenote.history.event.listener;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.payload.HistoryEvent;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class HistoryListener {

	private final UserHistoryRepository userHistoryRepository;

	public HistoryListener(UserHistoryRepository userHistoryRepository) {
		this.userHistoryRepository = userHistoryRepository;
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener
	public void registryReview(
		HistoryEvent event
	) {
		UserHistory save = userHistoryRepository.save(UserHistory.builder()
			.userId(event.userId())
			.alcoholId(event.alcoholId())
			.eventCategory(event.eventCategory())
			.eventType(event.eventType())
			.redirectUrl(event.redirectUrl())
			.imageUrl(event.imageUrl())
			.message(event.message())
			.dynamicMessage(event.dynamicMessage())
			.eventYear(String.valueOf(LocalDateTime.now().getYear()))
			.eventMonth(String.valueOf(LocalDateTime.now().getMonth()))
			.description(event.description())
			.build());

		log.info("History saved: {}", save);
	}
}
