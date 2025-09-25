package app.bottlenote.alcohols.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.repository.JpaAlcoholQueryRepository;
import app.bottlenote.shared.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.shared.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.shared.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.shared.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@DisplayName("[unit] [service] AlcoholQuery")
@ExtendWith(MockitoExtension.class)
class AlcoholQueryServiceTest {

  @Mock private JpaAlcoholQueryRepository jpaAlcoholQueryRepository;

  @InjectMocks private AlcoholQueryService alcoholQueryService;

  private Long userId;
  private AlcoholSearchRequest request;
  private PageResponse<AlcoholSearchResponse> response;

  @BeforeEach
  void setUp() {
    userId = 1L;
    request = AlcoholSearchRequest.builder().build();
    response = getResponse();
  }

  @Test
  @DisplayName("위스키 검색 할 수 있다.")
  void testSearchAlcohols() {
    // given
    // when
    when(jpaAlcoholQueryRepository.searchAlcohols(any(AlcoholSearchCriteria.class)))
        .thenReturn(response);
    PageResponse<AlcoholSearchResponse> actualResponse =
        alcoholQueryService.searchAlcohols(request, userId);

    // then
    assertEquals(response.content(), actualResponse.content());
    assertEquals(response.cursorPageable(), actualResponse.cursorPageable());
    verify(jpaAlcoholQueryRepository).searchAlcohols(any(AlcoholSearchCriteria.class));
  }

  private PageResponse<AlcoholSearchResponse> getResponse() {

    AlcoholsSearchItem detail_1 =
        AlcoholsSearchItem.builder()
            .alcoholId(5L)
            .korName("아녹 24년")
            .engName("anCnoc 24-year-old")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/6/6/989/270671-big.jpg")
            .rating(4.5)
            .ratingCount(1L)
            .reviewCount(0L)
            .pickCount(1L)
            .isPicked(false)
            .build();

    AlcoholsSearchItem detail_2 =
        AlcoholsSearchItem.builder()
            .alcoholId(1L)
            .korName("글래스고 1770 싱글몰트 스카치 위스키")
            .engName("1770 Glasgow Single Malt")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg")
            .rating(3.5)
            .ratingCount(3L)
            .reviewCount(1L)
            .pickCount(1L)
            .isPicked(false)
            .build();

    AlcoholsSearchItem detail_3 =
        AlcoholsSearchItem.builder()
            .alcoholId(2L)
            .korName("글래스고 1770 싱글몰트 스카치 위스키")
            .engName("1770 Glasgow Single Malt")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8888/404535-big.jpg")
            .rating(3.5)
            .ratingCount(1L)
            .reviewCount(0L)
            .pickCount(1L)
            .isPicked(false)
            .build();

    Long totalCount = 5L;
    List<AlcoholsSearchItem> details = List.of(detail_1, detail_2, detail_3);
    CursorPageable cursorPageable =
        CursorPageable.builder().currentCursor(0L).cursor(4L).pageSize(3L).hasNext(true).build();
    AlcoholSearchResponse response = AlcoholSearchResponse.of(totalCount, details);
    return PageResponse.of(response, cursorPageable);
  }
}
