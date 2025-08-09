package app.bottlenote.like.domain;

import java.util.List;
import java.util.Optional;

public interface LikesRepository {

  Likes save(Likes likes);

  Optional<Likes> findById(Long id);

  List<Likes> findAll();

  Optional<Likes> findByReviewIdAndUserId(Long reviewId, Long userId);
}
