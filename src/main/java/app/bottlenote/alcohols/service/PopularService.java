package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.repository.PopularQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularService {

	private final PopularQueryRepository popularQueryRepository;

	@Transactional
	public List<Populars> getPopularOfWeek(Integer top) {
		List<Populars> populars = List.of(
			Populars.of("1", "글렌피딕", "glen fi", "3.5", "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of("2", "맥키토시", "macintosh", "4.5", "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of("3", "글렌리벳", "glen rivet", "4.0", "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of("4", "글렌피딕", "glen fi", "3.5", "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of("5", "맥키토시", "macintosh", "4.5", "single molt", "https://i.imgur.com/TE2nmYV.png")
		);
		return populars;
	}
}
