package app.bottlenote.like.fixture;

import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryLikesRepository implements LikesRepository {
  private static final Logger log = LogManager.getLogger(InMemoryReviewRepository.class);
  Map<Long, Likes> database = new HashMap<>();

  @Override
  public Likes save(Likes likes) {
    Long id = (Long) ReflectionTestUtils.getField(likes, "id");
    if (id != null && database.containsKey(id)) {
      database.put(id, likes);
    } else {
      id = database.size() + 1L;
      database.put(id, likes);
      ReflectionTestUtils.setField(likes, "id", id);
    }
    log.info("[InMemory] likes repository save = {}", likes);
    return likes;
  }

  @Override
  public Optional<Likes> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Likes> findAll() {
    return List.copyOf(database.values());
  }

  @Override
  public Optional<Likes> findByReviewIdAndUserId(Long reviewId, Long userId) {
    Optional<Likes> first =
        database.values().stream()
            .filter(
                likes ->
                    likes.getReviewId().equals(reviewId)
                        && likes.getUserInfo().getUserId().equals(userId))
            .findFirst();

    log.info("[InMemory] likes repository findByReviewIdAndUserId = {}", first);

    return first;
  }
}
