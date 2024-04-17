package app.bottlenote.global.service.meta;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode
@ToString
@Getter
public class MetaInfos {
	private final Map<String, Object> metaInfos;

	protected MetaInfos() {
		metaInfos = new HashMap<>();
	}

	public Map<String, Object> add(String key, Object value) {
		// 테스트시 불편함으로 인행 주석 처리
		//Objects.requireNonNull(key, "key는 null이 될 수 없습니다.");
		//Objects.requireNonNull(value, "value 는 null이 될 수 없습니다.");

		metaInfos.put(key, value);
		return metaInfos;
	}

	public Object findByKey(String key) {
		return metaInfos.get(key);
	}
}
