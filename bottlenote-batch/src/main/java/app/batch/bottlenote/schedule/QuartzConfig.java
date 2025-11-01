package app.batch.bottlenote.schedule;


import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 스케줄러 설정을 위한 구성 클래스입니다.
 * <p>
 * 이 클래스는 Spring의 @Configuration 어노테이션을 통해 스케줄러 관련 빈들을 정의합니다.
 * Quartz에 필요한 JobDetail과 Trigger를 정의하여 배치 작업이 언제, 어떤 주기로 실행될지 관리합니다.
 * <p>
 * 각 작업마다 JobDetail 및 Trigger 구성을 별도로 만들어 관리하며, cron 표현식을 통해 실행 주기를 설정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

	/**
	 * 베스트 리뷰 선정 작업을 위한 JobDetail을 정의합니다.
	 * <p>
	 * JobDetail은 Quartz가 실행할 Job의 인스턴스와 관련 정보를 정의합니다.
	 * 해당 Job의 유형과 식별자를 지정하고, storeDurably() 메소드를 통해
	 * 트리거가 없어도 스케줄러가 종료되지 않도록 설정합니다.
	 *
	 * @return 베스트 리뷰 선정 작업을 위한 JobDetail 객체
	 */
	@Bean
	public JobDetail bestReviewJobDetail() {
		return JobBuilder.newJob(BestReviewQuartzJob.class)
				.withIdentity("bestReviewSelectedJob")
				.storeDurably()
				.build();
	}

	/**
	 * 베스트 리뷰 선정 작업을 위한 Trigger를 정의합니다.
	 * <p>
	 * Trigger는 작업이 언제 실행되어야 하는지를 정의합니다.
	 * 이 트리거는 매일 자정(0시 0분 0초)에 실행되도록 cron 표현식을 사용하여 설정합니다.
	 * "0 0 0 * * ?" 표현식은 매일 자정에 실행을 의미합니다.
	 *
	 * @return 베스트 리뷰 선정 작업을 위한 Trigger 객체
	 */
	@Bean
	public Trigger bestReviewJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(bestReviewJobDetail())
				.withIdentity("bestReviewSelectedTrigger")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
				.build();
	}

	/**
	 * 인기 위스키 선정 작업을 위한 JobDetail을 정의합니다.
	 * <p>
	 * JobDetail은 Quartz가 실행할 Job의 인스턴스와 관련 정보를 정의합니다.
	 * 해당 Job의 유형과 식별자를 지정하고, storeDurably() 메소드를 통해
	 * 트리거가 없어도 스케줄러가 종료되지 않도록 설정합니다.
	 *
	 * @return 인기 위스키 선정 작업을 위한 JobDetail 객체
	 */
	@Bean
	public JobDetail popularAlcoholJobDetail() {
		return JobBuilder.newJob(PopularAlcoholQuartzJob.class)
				.withIdentity("popularReviewSelectedJob")
				.storeDurably()
				.build();
	}

	/**
	 * 인기 위스키 선정 작업을 위한 Trigger를 정의합니다.
	 * <p>
	 * Trigger는 작업이 언제 실행되어야 하는지를 정의합니다.
	 * 이 트리거는 매일 자정(0시 0분 0초)에 실행되도록 cron 표현식을 사용하여 설정합니다.
	 * "0 0 0 * * ?" 표현식은 매일 자정에 실행을 의미합니다.
	 *
	 * @return 인기 위스키 선정 작업을 위한 Trigger 객체
	 */
	@Bean
	public Trigger popularAlcoholJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(popularAlcoholJobDetail())
				.withIdentity("popularReviewSelectedTrigger")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
				.build();
	}

	/**
	 * 일일 데이터 리포트 작업을 위한 JobDetail을 정의합니다.
	 * <p>
	 * JobDetail은 Quartz가 실행할 Job의 인스턴스와 관련 정보를 정의합니다.
	 * 해당 Job의 유형과 식별자를 지정하고, storeDurably() 메소드를 통해
	 * 트리거가 없어도 스케줄러가 종료되지 않도록 설정합니다.
	 *
	 * @return 일일 데이터 리포트 작업을 위한 JobDetail 객체
	 */
	@Bean
	public JobDetail dailyDataReportJobDetail() {
		return JobBuilder.newJob(DailyDataReportQuartzJob.class)
				.withIdentity("dailyDataReportJob")
				.storeDurably()
				.build();
	}

	/**
	 * 일일 데이터 리포트 작업을 위한 Trigger를 정의합니다.
	 * <p>
	 * Trigger는 작업이 언제 실행되어야 하는지를 정의합니다.
	 * 이 트리거는 매일 오전 10시(10시 0분 0초)에 실행되도록 cron 표현식을 사용하여 설정합니다.
	 * "0 0 10 * * ?" 표현식은 매일 오전 10시에 실행을 의미합니다.
	 *
	 * @return 일일 데이터 리포트 작업을 위한 Trigger 객체
	 */
	@Bean
	public Trigger dailyDataReportJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(dailyDataReportJobDetail())
				.withIdentity("dailyDataReportTrigger")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 10 * * ?"))
				.build();
	}
}
