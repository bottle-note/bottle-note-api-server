package app.bottlenote.banner.repository;

import app.bottlenote.banner.dto.request.AdminBannerSearchRequest;
import app.bottlenote.banner.dto.response.AdminBannerListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomBannerRepository {

  Page<AdminBannerListResponse> searchForAdmin(AdminBannerSearchRequest request, Pageable pageable);
}
