package app.bottlenote.support.help.repository.custom;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.repository.HelpQuerySupporter;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.support.help.domain.QHelp.help;

@Slf4j
@RequiredArgsConstructor
public class CustomHelpQueryRepositoryImpl implements CustomHelpQueryRepository{

	private final JPAQueryFactory queryFactory;
	private final HelpQuerySupporter supporter;

	@Override
	public PageResponse<HelpListResponse> getHelpList(HelpPageableRequest helpPageableRequest, Long currentUserId) {

		List<HelpListResponse.HelpInfo> fetch = queryFactory
			.select(supporter.helpResponseConstructor())
			.from(help)
			.where(help.userId.eq(currentUserId))
			.offset(helpPageableRequest.cursor())
			.limit(helpPageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(help.id.count())
			.from(help)
			.where(help.userId.eq(currentUserId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(helpPageableRequest, fetch);
		log.info("CURSOR Pageable info :{}", cursorPageable.toString());

		return PageResponse.of(HelpListResponse.of(totalCount, fetch), cursorPageable);

	}

	private CursorPageable getCursorPageable(
		HelpPageableRequest helpPageableRequest,
		List<HelpListResponse.HelpInfo> fetch
	) {

		boolean hasNext = isHasNext(helpPageableRequest, fetch);
		return CursorPageable.builder()
			.cursor(helpPageableRequest.cursor() + helpPageableRequest.pageSize())
			.pageSize(helpPageableRequest.pageSize())
			.hasNext(hasNext)
			.currentCursor(helpPageableRequest.cursor())
			.build();
	}

	/**
	 * 다음 페이지가 있는지 확인하는 메소드
	 */
	private boolean isHasNext(
		HelpPageableRequest helpPageableRequest,
		List<HelpListResponse.HelpInfo> fetch
	) {
		boolean hasNext = fetch.size() > helpPageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}
}
