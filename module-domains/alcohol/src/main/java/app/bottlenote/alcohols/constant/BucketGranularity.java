package app.bottlenote.alcohols.constant;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/** 인기도 관측 기간의 단위와 경계 계산 규칙. */
public enum BucketGranularity {
  HOUR {
    @Override
    LocalDateTime resolveStartAt(LocalDateTime dateTime) {
      return dateTime.truncatedTo(ChronoUnit.HOURS);
    }

    @Override
    LocalDateTime resolveEndAt(LocalDateTime startAt) {
      return startAt.plusHours(1);
    }
  },
  WEEK {
    @Override
    LocalDateTime resolveStartAt(LocalDateTime dateTime) {
      return dateTime
          .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
          .toLocalDate()
          .atStartOfDay();
    }

    @Override
    LocalDateTime resolveEndAt(LocalDateTime startAt) {
      return startAt.plusWeeks(1);
    }
  },
  MONTH {
    @Override
    LocalDateTime resolveStartAt(LocalDateTime dateTime) {
      return dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay();
    }

    @Override
    LocalDateTime resolveEndAt(LocalDateTime startAt) {
      return startAt.plusMonths(1);
    }
  };

  public LocalDateTime startAt(LocalDateTime dateTime) {
    return resolveStartAt(Objects.requireNonNull(dateTime, "dateTime은 null일 수 없습니다."));
  }

  public LocalDateTime endAt(LocalDateTime startAt) {
    LocalDateTime normalizedStartAt = startAt(startAt);
    if (!normalizedStartAt.equals(startAt)) {
      throw new IllegalArgumentException("startAt은 버킷 시작 시각이어야 합니다.");
    }
    return resolveEndAt(startAt);
  }

  abstract LocalDateTime resolveStartAt(LocalDateTime dateTime);

  abstract LocalDateTime resolveEndAt(LocalDateTime startAt);
}
