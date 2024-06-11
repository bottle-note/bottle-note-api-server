package app.bottlenote.global.cache.local;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalCacheType {
	LOCAL_REGION_CACHE("LC-Region", 60 * 60 * 24, 1),
	LOCAL_ALCOHOL_CATEGORY_CACHE("LC-AlcoholCategory", 60 * 60 * 24 * 7, 1);

	private final String cacheName; // 등록할 캐시 이름
	private final int secsToExpireAfterWrite; // 캐시 만료 시간 ( 60 * 60 * 24  = 1일 )
	private final int entryMaxSize; // 캐시 최대 크기
}
