package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Distillery;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DistilleryTestFactory {

  private static final AtomicInteger counter = new AtomicInteger(0);

  @Autowired private EntityManager em;

  public static Distillery createDistillery(String korName, String engName) {
    return createDistillery(korName, engName, null);
  }

  public static Distillery createDistillery(String korName, String engName, String logoImgUrl) {
    return Distillery.builder().korName(korName).engName(engName).logoImgPath(logoImgUrl).build();
  }

  @Transactional
  public Distillery persistDistillery() {
    Distillery d =
        Distillery.builder()
            .korName("증류소-" + nextSuffix())
            .engName("Distillery-" + nextSuffix())
            .build();
    em.persist(d);
    em.flush();
    return d;
  }

  @Transactional
  public Distillery persistDistillery(String korName, String engName) {
    Distillery d = Distillery.builder().korName(korName).engName(engName).build();
    em.persist(d);
    em.flush();
    return d;
  }

  private String nextSuffix() {
    return String.valueOf(counter.incrementAndGet());
  }
}
