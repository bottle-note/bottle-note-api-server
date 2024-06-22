package app.bottlenote.review.repository;

import app.bottlenote.review.domain.ReviewTastingTag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewTastingTagRepository extends CrudRepository<ReviewTastingTag, Long> {
}
