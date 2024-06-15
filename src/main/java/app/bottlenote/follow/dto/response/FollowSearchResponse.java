package app.bottlenote.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class FollowSearchResponse {

	private final Long totalCount;
	private final List<FollowDetail> followList;


}
