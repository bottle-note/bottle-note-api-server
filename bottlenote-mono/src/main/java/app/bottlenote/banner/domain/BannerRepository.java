package app.bottlenote.banner.domain;

import java.util.List;
import java.util.Optional;

public interface BannerRepository {

	Banner save(Banner banner);

	Optional<Banner> findById(Long id);

	List<Banner> findAllByIsActiveTrue();
}
