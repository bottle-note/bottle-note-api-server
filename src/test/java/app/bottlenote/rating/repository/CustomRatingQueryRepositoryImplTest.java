package app.bottlenote.rating.repository;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.domain.constant.SearchSortType;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class CustomRatingQueryRepositoryImplTest {

	@Autowired
	private RatingRepository ratingRepository;


	@Test
	void test() {
		var criteria = new RatingListFetchCriteria(
			"Yam",
			null,
			null,
			SearchSortType.REVIEW,
			SortOrder.DESC,
			0L,
			10L,
			3L
		);

		var response = ratingRepository.fetchRatingList(criteria);

		RatingListFetchResponse content = response.content();

		content.ratings().forEach(info ->
			System.out.printf("ID: %d\nImage URL: %s\nKorean Name: %s\nEnglish Name: %s\nKorean Category Name: %s\nEnglish Category Name: %s\nIs Picked: %b\n\n",
				info.alcoholId(),
				info.imageUrl(),
				info.korName(),
				info.engName(),
				info.korCategoryName(),
				info.engCategoryName(),
				info.isPicked()
			)
		);

		//when
		//then
	}
}
