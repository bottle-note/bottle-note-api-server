package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.domain.MfdsMatchingSelectionRepository;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
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

  @Override
  public List<MfdsMatchingSelection> findByDeclarationIdInOrderBySelectedAtDescIdDesc(
      Collection<Long> declarationIds) {
    return selections.stream()
        .filter(selection -> declarationIds.contains(selection.getDeclarationId()))
        .sorted(
            Comparator.comparing(
                    MfdsMatchingSelection::getSelectedAt,
                    Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
                .thenComparing(
                    selection -> selection.getId() == null ? 0L : selection.getId(),
                    Comparator.reverseOrder()))
        .toList();
  }
}
