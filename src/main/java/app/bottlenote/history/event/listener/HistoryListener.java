package app.bottlenote.history.event.listener;

import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.dto.payload.HistoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryListener {

	private final AlcoholFacade alcoholFacade;
	private final UserHistoryRepository userHistoryRepository;

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener
	public void registryUserHistory(
		HistoryEvent event
	) {
		String alcoholImageUrl = alcoholFacade.findAlcoholImageUrlById(event.alcoholId()).orElse(null);

		UserHistory save = userHistoryRepository.save(UserHistory.builder()
			.userId(event.userId())
			.alcoholId(event.alcoholId())
			.eventCategory(event.eventCategory())
			.eventType(event.eventType())
			.redirectUrl(event.redirectUrl())
			.imageUrl(alcoholImageUrl)
			.message(event.message())
			.dynamicMessage(event.dynamicMessage())
			.eventYear(String.valueOf(LocalDateTime.now().getYear()))
			.eventMonth(String.valueOf(LocalDateTime.now().getMonth()))
			.description(event.description())
			.build());

		log.info("History saved: {}", save);
	}
}
