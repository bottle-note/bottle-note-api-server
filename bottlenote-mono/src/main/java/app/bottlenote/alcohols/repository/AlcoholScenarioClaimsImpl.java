package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.common.domain.AuditPrincipal;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;

@RequiredArgsConstructor
public class AlcoholScenarioClaimsImpl implements AlcoholScenarioClaims {
  private final EntityManager em;
  private final AuditorAware<AuditPrincipal> auditor;

  public void claimScenario(AlcoholAttributeScenario s, long expected) {
    claim("alcohol_attribute_scenario", s.getId(), expected);
    em.refresh(s);
  }

  public void claimVersion(AlcoholAttributeScenarioVersion v, long expected) {
    claim("alcohol_attribute_scenario_version", v.getId(), expected);
    em.refresh(v);
  }

  private void claim(String entity, Long id, long expected) {
    int changed =
        em.createQuery(
                "update "
                    + entity
                    + " e set e.lockVersion=e.lockVersion+1, e.lastModifyAt=:now, e.lastModifyPrincipal=:principal where e.id=:id and e.lockVersion=:expected")
            .setParameter("now", LocalDateTime.now())
            .setParameter("principal", auditor.getCurrentAuditor().orElse(null))
            .setParameter("id", id)
            .setParameter("expected", expected)
            .executeUpdate();
    if (changed != 1) throw new ScenarioException(SCENARIO_CONFLICT);
  }
}
