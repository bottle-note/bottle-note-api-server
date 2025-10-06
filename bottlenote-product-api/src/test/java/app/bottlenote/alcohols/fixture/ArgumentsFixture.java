package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class ArgumentsFixture {
  // 테스트 케이스 fixture data

  static Stream<Arguments> testCase1Provider() {
    return Stream.of(
        Arguments.of(
            "모든 요청 파라미터가 존재할 때.",
            AlcoholSearchRequest.builder()
                .keyword("glen")
                .category(AlcoholCategoryGroup.SINGLE_MALT)
                .regionId(1L)
                .sortType(SearchSortType.REVIEW)
                .sortOrder(SortOrder.DESC)
                .cursor(0L)
                .pageSize(3L)
                .build()),
        Arguments.of(
            "키워드가 없을 때.",
            AlcoholSearchRequest.builder()
                .keyword("")
                .category(AlcoholCategoryGroup.SINGLE_MALT)
                .regionId(1L)
                .sortType(SearchSortType.REVIEW)
                .sortOrder(SortOrder.DESC)
                .cursor(0L)
                .pageSize(3L)
                .build()),
        Arguments.of(
            "카테고리도 없을 때.",
            AlcoholSearchRequest.builder()
                .keyword("")
                .category(null)
                .regionId(1L)
                .sortType(SearchSortType.REVIEW)
                .sortOrder(SortOrder.DESC)
                .cursor(0L)
                .pageSize(3L)
                .build()),
        Arguments.of(
            "지역 아이디도 없을 때.",
            AlcoholSearchRequest.builder()
                .keyword("")
                .category(null)
                .regionId(null)
                .sortType(SearchSortType.REVIEW)
                .sortOrder(SortOrder.DESC)
                .cursor(0L)
                .pageSize(3L)
                .build()),
        Arguments.of(
            "정렬 정보도 없을 때.",
            AlcoholSearchRequest.builder()
                .keyword("")
                .category(null)
                .regionId(null)
                .sortType(null)
                .sortOrder(null)
                .cursor(0L)
                .pageSize(3L)
                .build()),
        Arguments.of(
            "페이지 정보도도 없을 때.",
            AlcoholSearchRequest.builder()
                .keyword("")
                .category(null)
                .regionId(null)
                .sortType(null)
                .sortOrder(null)
                .cursor(null)
                .pageSize(null)
                .build()));
  }

  static Stream<Arguments> sortTypeParameters() {
    return Stream.of(
        // 성공 케이스
        Arguments.of("REVIEW", 200),
        Arguments.of("POPULAR", 200),
        Arguments.of("PICK", 200),
        Arguments.of("REVIEW", 200),
        // 실패 케이스
        Arguments.of("RATINGS", 400),
        Arguments.of("POpu", 400),
        Arguments.of("PIC", 400),
        Arguments.of("REVIEWWW", 400));
  }

  static Stream<Arguments> sortOrderParameters() {
    return Stream.of(
        // 성공 케이스
        Arguments.of("ASC", 200),
        Arguments.of("DESC", 200),
        // 실패 케이스
        Arguments.of("DESCCC", 400),
        Arguments.of("ASCC", 400));
  }
}
