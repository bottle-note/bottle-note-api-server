package app.bottlenote.follow.service;

import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.repository.follower.FollowerRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FollowerService {

	private final FollowerRepository followerRepository;

	@Transactional(readOnly = true)
	public PageResponse<FollowSearchResponse> findFollowerList(Long userId, FollowPageableRequest pageableRequest) {

		FollowPageableCriteria criteria = FollowPageableCriteria.of(
			pageableRequest.cursor(),
			pageableRequest.pageSize(),
			userId
		);

		return followerRepository.followerList(criteria);


	}
}
