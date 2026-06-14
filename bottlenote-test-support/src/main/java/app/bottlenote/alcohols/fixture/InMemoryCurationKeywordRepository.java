package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.domain.CurationKeywordRepository;
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCurationKeywordRepository implements CurationKeywordRepository {

  private final List<CurationKeyword> curations = new ArrayList<>();

  @Override
  public Optional<CurationKeyword> findById(Long id) {
    return curations.stream().filter(curation -> curation.getId().equals(id)).findFirst();
  }

  @Override
  public Optional<CurationKeyword> findByNameContainingAndIsActiveTrue(String name) {
    return Optional.empty();
  }

  @Override
  public CursorResponse<CurationKeywordResponse> searchCurationKeywords(
      String keyword, Long alcoholId, Long cursor, Integer pageSize) {
    return null;
  }

  @Override
  public CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Integer pageSize) {
    return null;
  }

  @Override
  public Optional<Set<Long>> findAlcoholIdsByKeyword(String keyword) {
    return Optional.empty();
  }

  @Override
  public CurationKeyword save(CurationKeyword curationKeyword) {
    if (curationKeyword.getId() == null) {
      ReflectionTestUtils.setField(curationKeyword, "id", (long) (curations.size() + 1));
    }
    curations.removeIf(curation -> curation.getId().equals(curationKeyword.getId()));
    curations.add(curationKeyword);
    return curationKeyword;
  }

  @Override
  public void delete(CurationKeyword curationKeyword) {
    curations.removeIf(curation -> curation.getId().equals(curationKeyword.getId()));
  }

  @Override
  public boolean existsByName(String name) {
    return curations.stream().anyMatch(curation -> curation.getName().equals(name));
  }

  @Override
  public Page<AdminCurationListResponse> searchForAdmin(
      AdminCurationSearchRequest request, Pageable pageable) {
    return new PageImpl<>(List.of(), pageable, 0);
  }

  @Override
  public List<CurationKeyword> findAllOrderByDisplayOrderAsc() {
    return curations.stream()
        .sorted(
            Comparator.comparing(CurationKeyword::getDisplayOrder)
                .thenComparing(CurationKeyword::getId))
        .toList();
  }
}
