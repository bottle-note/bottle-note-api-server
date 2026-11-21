package app.bottlenote.support.help.fixture;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpRepository;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryHelpRepository implements HelpRepository {

  private final Map<Long, Help> database = new HashMap<>();
  private long sequence = 1L;

  @Override
  public Help save(Help help) {
    Long id = help.getId();
    if (Objects.isNull(id)) {
      id = sequence++;
      ReflectionTestUtils.setField(help, "id", id);
    }
    database.put(id, help);
    return help;
  }

  @Override
  public Optional<Help> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Help> findAll() {
    return List.copyOf(database.values());
  }

  @Override
  public List<Help> findAllByUserId(Long userId) {
    return database.values().stream().filter(h -> h.getUserId().equals(userId)).toList();
  }

  @Override
  public List<Help> findAllByUserIdAndType(Long userId, HelpType helpType) {
    return database.values().stream()
        .filter(h -> h.getUserId().equals(userId) && h.getType().equals(helpType))
        .toList();
  }

  @Override
  public Optional<Help> findByIdAndUserId(Long id, Long userId) {
    return database.values().stream()
        .filter(h -> h.getId().equals(id) && h.getUserId().equals(userId))
        .findFirst();
  }

  @Override
  public KeysetPageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId) {
    return KeysetPageResponse.of(HelpListResponse.of(List.of()), new KeysetPagination(false, null));
  }

  @Override
  public Page<AdminHelpListResponse.AdminHelpInfo> getAdminHelpList(
      AdminHelpPageableRequest request) {
    List<AdminHelpListResponse.AdminHelpInfo> filtered =
        database.values().stream()
            .filter(help -> request.status() == null || request.status() == help.getStatus())
            .filter(help -> request.type() == null || request.type() == help.getType())
            .sorted(
                Comparator.comparing(
                        Help::getCreateAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Help::getId, Comparator.reverseOrder()))
            .map(
                help ->
                    AdminHelpListResponse.AdminHelpInfo.builder()
                        .helpId(help.getId())
                        .userId(help.getUserId())
                        .userNickname(null)
                        .title(help.getTitle())
                        .type(help.getType())
                        .status(help.getStatus())
                        .createAt(help.getCreateAt())
                        .build())
            .toList();
    int from = Math.min(request.page() * request.size(), filtered.size());
    int to = Math.min(from + request.size(), filtered.size());
    return new PageImpl<>(
        filtered.subList(from, to),
        PageRequest.of(request.page(), request.size()),
        filtered.size());
  }

  public void clear() {
    database.clear();
    sequence = 1L;
  }
}
