package app.bottlenote.follow.repository.follower;

import app.bottlenote.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowerRepository extends JpaRepository<Follow, Long>, CustomFollowerRepository  {


}
