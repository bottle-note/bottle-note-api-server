package app.bottlenote.notification.fixture;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** 인메모리 테스트의 콜백만 실행하며 실제 롤백은 통합 테스트에서 검증한다. */
public class FakeNotificationTransactionManager extends AbstractPlatformTransactionManager {
  @Override
  protected Object doGetTransaction() {
    return new Object();
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {}

  @Override
  protected void doCommit(DefaultTransactionStatus status) {}

  @Override
  protected void doRollback(DefaultTransactionStatus status) {}
}
