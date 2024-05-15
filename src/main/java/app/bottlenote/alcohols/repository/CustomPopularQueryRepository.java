package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.response.Populars;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomPopularQueryRepository {
	List<Populars> getPopularOfWeek(Integer size, Long userId);
}
