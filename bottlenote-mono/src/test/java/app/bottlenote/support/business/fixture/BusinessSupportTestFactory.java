package app.bottlenote.support.business.fixture;

import app.bottlenote.support.business.constant.BusinessSupportType;
import app.bottlenote.support.business.domain.BusinessSupport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BusinessSupportTestFactory {

  @Autowired private EntityManager em;

  @Transactional
  @NotNull
  public BusinessSupport persist(@NotNull Long userId) {
    BusinessSupport bs =
        BusinessSupport.create(
            userId, "이벤트 협업 관련 문의드려요", "blah blah", "test@naver.com", BusinessSupportType.EVENT);
    em.persist(bs);
    em.flush();
    return bs;
  }
}
