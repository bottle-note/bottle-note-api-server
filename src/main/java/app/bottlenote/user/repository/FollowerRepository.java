package app.bottlenote.user.repository;

import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.repository.custom.CustomFollowerRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowerRepository extends JpaRepository<Follow, Long>, CustomFollowerRepository {

}
