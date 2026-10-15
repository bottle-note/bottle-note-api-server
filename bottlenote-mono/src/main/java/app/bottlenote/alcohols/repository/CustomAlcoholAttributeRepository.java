package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.dsl.AlcoholAttributeSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 위스키 속성의 QueryDSL 조회 조각. */
public interface CustomAlcoholAttributeRepository {

  Page<Long> findAlcoholIds(AlcoholAttributeSearchCondition condition, Pageable pageable);
}
