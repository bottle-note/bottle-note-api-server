package app.bottlenote.support.business.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record BusinessSupportUpsertRequest(
		@Size(max = 500)
		String content,

		String contactWay
) {
}

