package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor(staticName = "of")
public class AlcoholSearchResponse {
	private final Long totalCount;
	private final List<AlcoholsSearchDetail> alcohols;
}
