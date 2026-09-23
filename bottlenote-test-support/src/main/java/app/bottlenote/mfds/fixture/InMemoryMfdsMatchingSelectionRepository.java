package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.domain.MfdsMatchingSelectionRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryMfdsMatchingSelectionRepository implements MfdsMatchingSelectionRepository {
  private final List<MfdsMatchingSelection> selections = new ArrayList<>();

  @Override
  public MfdsMatchingSelection save(MfdsMatchingSelection selection) {
    selections.add(selection);
    return selection;
  }

  public List<MfdsMatchingSelection> findAll() {
    return List.copyOf(selections);
  }
}
