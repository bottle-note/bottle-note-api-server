package app.bottlenote.statistics.domain;

import java.time.LocalDateTime;

/** 같은 버킷에 가입한 회원 묶음. */
public record MemberCohort(LocalDateTime cohortAt, long members) {}
