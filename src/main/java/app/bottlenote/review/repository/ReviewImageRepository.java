package app.bottlenote.review.repository;

import app.bottlenote.review.domain.ReviewImage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends CrudRepository<ReviewImage, Long> {

}
