package app.bottlenote.follow.repository;

import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.global.service.cursor.CursorPageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FollowQuerySupporter {

	public CursorPageable getCursorPageable(FollowPageableCriteria criteria, List<FollowDetail> followDetails) {
		boolean hasNext = isHasNext(criteria, followDetails);
		return CursorPageable.builder()
			.cursor(criteria.cursor() + criteria.pageSize())
			.pageSize(criteria.pageSize())
			.hasNext(hasNext)
			.currentCursor(criteria.cursor())
			.build();
	}

	private boolean isHasNext(FollowPageableCriteria criteria, List<FollowDetail> fetch) {
		boolean hasNext = fetch.size() > criteria.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
