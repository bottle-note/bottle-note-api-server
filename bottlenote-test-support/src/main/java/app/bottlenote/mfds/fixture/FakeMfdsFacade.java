package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.dto.response.MfdsPublicDeclarationItem;
import app.bottlenote.mfds.facade.MfdsFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** MfdsFacade 테스트 더블. */
public class FakeMfdsFacade implements MfdsFacade {

  private final Map<Long, List<MfdsPublicDeclarationItem>> byAlcoholId = new ConcurrentHashMap<>();

  public void put(Long alcoholId, List<MfdsPublicDeclarationItem> items) {
    byAlcoholId.put(alcoholId, new ArrayList<>(items));
  }

  @Override
  public List<MfdsPublicDeclarationItem> findVerifiedDeclarationsByAlcoholId(Long alcoholId) {
    return List.copyOf(byAlcoholId.getOrDefault(alcoholId, List.of()));
  }
}
