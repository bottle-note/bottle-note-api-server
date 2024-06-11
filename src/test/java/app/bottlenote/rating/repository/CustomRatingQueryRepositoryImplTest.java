package app.bottlenote.rating.repository;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.domain.constant.SearchSortType;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;

/**
 * 해당 테스트는 실제로 어플리케이션을 띄워 실행 결과를 볼수 있는 예시라고 생각하고 참조하세요.
 * 테스트를 실행하려면 해당 테스트를 실행할 수 있는 환경이 필요합니다.
 */
//@SpringBootTest
//@ActiveProfiles("dev")
class CustomRatingQueryRepositoryImplTest {

	//@Autowired
	private RatingRepository ratingRepository;


	//@Test
	void test() {
		var criteria = new RatingListFetchCriteria(
			"Yam",
			null,
			null,
			SearchSortType.REVIEW,
			SortOrder.DESC,
			0L,
			10L,
			33L
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
