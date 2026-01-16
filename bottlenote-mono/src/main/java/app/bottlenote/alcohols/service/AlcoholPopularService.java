package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.PopularQueryRepository;
import app.bottlenote.alcohols.dto.response.PopularItem;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholPopularService {

  private final PopularQueryRepository popularQueryRepository;

  @Transactional(readOnly = true)
  public List<PopularItem> getPopularOfWeek(Integer top, Long userId) {
    PageRequest pageRequest = PageRequest.of(0, top);
    List<PopularItem> popularItemList =
        popularQueryRepository.getPopularOfWeeks(userId, pageRequest);
    Collections.shuffle(popularItemList);
    return popularItemList;
  }

  @Transactional(readOnly = true)
  public List<PopularItem> getSpringItems(Long userId) {
    Pageable pageable = Pageable.ofSize(6);
    List<Long> tags =
        List.of(
            1L, 5L, 8L, 9L, 10L, 11L, 14L, 16L, 17L, 19L, 22L, 23L, 29L, 33L, 35L, 42L, 47L, 48L,
            54L, 60L, 62L, 66L, 70L, 72L, 73L, 75L, 80L, 88L, 94L);
    List<Long> excludedTags =
        List.of(
            34L, 41L, 45L, 49L, 63L, 102L, 105L, 118L, 139L, 140L, 153L, 162L, 167L, 170L, 172L);
    return popularQueryRepository.getSpringItems(userId, tags, excludedTags, pageable);
  }

  @Transactional(readOnly = true)
  public List<PopularItem> getPopularByViewsWeekly(Integer top, Long userId) {
    return popularQueryRepository.getPopularByViewsWeekly(userId, top);
  }

  @Transactional(readOnly = true)
  public List<PopularItem> getPopularByViewsMonthly(Integer top, Long userId) {
    return popularQueryRepository.getPopularByViewsMonthly(userId, top);
  }
}
