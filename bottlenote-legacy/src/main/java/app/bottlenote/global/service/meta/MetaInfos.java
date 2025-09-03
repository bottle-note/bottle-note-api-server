package app.bottlenote.global.service.meta;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class MetaInfos {
  // todo: product-api module로 이동. ( api 레벨에서만 사용하기 때문에 )
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
