package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCurationKeywordRepository {

  Optional<Set<Long>> findAlcoholIdsByKeyword(String keyword);

  Page<AdminCurationListResponse> searchForAdmin(
      AdminCurationSearchRequest request, Pageable pageable);
}
