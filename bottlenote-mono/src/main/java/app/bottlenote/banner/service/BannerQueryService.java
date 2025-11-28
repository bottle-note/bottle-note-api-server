package app.bottlenote.banner.service;

import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.banner.dto.response.BannerResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerQueryService {

  private final BannerRepository bannerRepository;

  public List<BannerResponse> getActiveBanners(Integer limit) {
    return bannerRepository.findAllByIsActiveTrue().stream()
        .sorted(Comparator.comparing(banner -> banner.getSortOrder()))
        .limit(limit)
        .map(BannerResponse::from)
        .toList();
  }
}
