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

		metaInfos.put(key, value);
		return metaInfos;
	}

	public Object findByKey(String key) {
		return metaInfos.get(key);
	}
}
