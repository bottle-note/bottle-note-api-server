package app.bottlenote.global.service.meta;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
public class MetaInfos {
	private final Map<String, Object> metaInfos;

	protected MetaInfos() {
		metaInfos = new HashMap<>();
	}
	public Object findByKey(String key) {
		return metaInfos.get(key);
	}

	public MetaInfos add(String key, Object value) {
		this.getMetaInfos().put(key, value);
		return this;
	}
}
