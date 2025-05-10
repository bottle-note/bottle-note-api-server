package app.bottlenote.history.scheduled;

import app.bottlenote.history.service.AlcoholViewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewHistorySyncJob implements Job {
	private final AlcoholViewHistoryService viewHistorySyncService;

	@Override
	@Transactional
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			viewHistorySyncService.syncViewHistoryFromRedisToDb();
		} catch (Exception e) {
			log.error("ViewHistorySyncJob 실패: ", e);
			throw new JobExecutionException(e);
		}
	}
}
