package app.bottlenote.alcohols.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Collections.emptyList;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols/explore")
public class AlcoholExploreController {

    // private final AlcoholExploreService alcoholExploreService;

    /**
     * 기본 표준 형태의 위스키 둘러보기 API
     * 인기도, 최신 업데이트, 카테고리 등을 종합적으로 고려한 둘러보기 목록 제공
     */
    @GetMapping("/standard")
    public ResponseEntity<?> getStandardExplore(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long cursor
    ) {
        //Long userId = getUserIdByContext().orElse(-1L);
        //PageResponse<AlcoholExploreResponse> pageResponse = alcoholExploreService.getStandardExplore(size, cursor, userId);

        //return GlobalResponse.ok( pageResponse.content(), MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
        return GlobalResponse.ok(emptyList());
    }
}
