package app.bottlenote.statistics.domain;

import java.time.LocalDateTime;

/** 코호트 시작으로부터 offset번째 버킷에 활동한 회원 수. offset 0은 가입 버킷 자신이다. */
public record MemberCohortActivity(LocalDateTime cohortAt, int offset, long activeMembers) {}
