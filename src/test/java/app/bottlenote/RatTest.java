package app.bottlenote;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RatTest {

	@Test
	void call() throws InterruptedException, ExecutionException {
		int count = 10000;
		int simultaneousWorkPossible = 20;

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

			Semaphore semaphore = new Semaphore(simultaneousWorkPossible);
			AtomicInteger taskCounter = new AtomicInteger(1);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

			List<Future<?>> futures = new ArrayList<>();

			for (int i = 0; i < count; i++) {
				int taskId = taskCounter.getAndIncrement();
				String requestTime = LocalTime.now().format(formatter);

				semaphore.acquire();
				futures.add(executor.submit(() -> {
					String startTime = LocalTime.now().format(formatter);
					int duration = ThreadLocalRandom.current().nextInt(15, 21);
					log("할당", taskId, requestTime, startTime, duration);

					try {
						Thread.sleep(duration * 1000L);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						String endTime = LocalTime.now().format(formatter);
						log("해제", taskId, endTime, null, 0);
						semaphore.release();
					}
				}));

				Thread.sleep(1200); // 작업간 1.2초 지연
			}

			for (Future<?> future : futures) {
				future.get();
			}

			executor.shutdown();
		}
	}

	void log(String type, int taskId, String time1, String time2, int duration) {
		if (type.equals("할당")) {
			System.out.printf("[] [Task-%d] 요청시간: %s, 처리 시작: %s, 처리 예상: %d초%n", taskId, time1, time2, duration);
		} else {
			System.out.printf("[-] [Task-%d] 종료 및 삭제 시간: %s%n", taskId, time1);
		}
	}
}
