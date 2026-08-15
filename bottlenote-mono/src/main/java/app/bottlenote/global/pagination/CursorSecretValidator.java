package app.bottlenote.global.pagination;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app", name = "type", havingValue = "product")
public class CursorSecretValidator implements InitializingBean {

  private final CursorProperties properties;

  public CursorSecretValidator(CursorProperties properties) {
    this.properties = properties;
  }

  @Override
  public void afterPropertiesSet() {
    properties.validate();
  }
}
