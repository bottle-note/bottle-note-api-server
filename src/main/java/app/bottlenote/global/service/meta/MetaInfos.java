package app.bottlenote.global.service.meta;

import lombok.Getter;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;


@Getter
public class MetaInfos {
	private final HashSet<Map<String, Object>> metaInfos;

	protected MetaInfos() {
		metaInfos = new HashSet<>();
	}

	/**
	 * 추가적인 메타 정보를 추가한다.
	 *
	 * @param key   the key
	 * @param value the value
	 */
	public MetaInfos add(String key, Object value) {
		Objects.requireNonNull(key, "메타정보의 key는 null이 될 수 없습니다.");
		Objects.requireNonNull(value, "메타정보의 value는 null이 될 수 없습니다.");

		metaInfos.add( Map.of(key, value));
		return this;
	}

	/**
	 * 메타 정보에서 key에 해당하는 값을 찾아 반환한다.
	 *
	 * @param key 검색 대상이 될 key값
	 * @return the map
	 */
	public Map<String, ?> findByKey(String key) {
		return metaInfos.stream()
			.filter(metaInfo -> metaInfo.containsKey(key))
			.findFirst()
			.orElse(Map.of());
	}
}
