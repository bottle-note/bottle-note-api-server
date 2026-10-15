package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 위스키 속성의 QueryDSL 조회 조각. */
public interface CustomAlcoholAttributeRepository {

  Page<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request, Pageable pageable);
}
