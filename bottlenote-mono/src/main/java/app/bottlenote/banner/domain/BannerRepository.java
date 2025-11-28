package app.bottlenote.banner.domain;

import java.util.List;
import java.util.Optional;

/**
 * 배너 도메인 레포지토리 (조회 전용)
 * <p>
 * 이 모듈은 사용자용 API로 조회만 가능합니다.
 * 배너 생성/수정/삭제는 Admin 모듈에서 처리합니다.
 */
public interface BannerRepository {

  Optional<Banner> findById(Long id);

  List<Banner> findAllByIsActiveTrue();
}
