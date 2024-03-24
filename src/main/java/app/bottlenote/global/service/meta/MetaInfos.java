package app.bottlenote.global.service.meta;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Getter
public class MetaInfos {
	private final Map<String, Object> metaInfos;

	protected MetaInfos() {
		metaInfos = new HashMap<>();
	}

	public Map<String, Object> add(String key, Object value) {
		Objects.requireNonNull(key, "key는 null이 될 수 없습니다.");
		Objects.requireNonNull(value, "value 는 null이 될 수 없습니다.");

		metaInfos.put(key, value);
		return metaInfos;
	}

	public Object findByKey(String key) {
		return metaInfos.get(key);
	}
}
