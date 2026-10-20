package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSummaryRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface AdminAlcoholAttributeService {
  Page<AdminAlcoholAttributeItem> findAll(AdminAlcoholAttributeSearchRequest request);

  List<AdminAlcoholAttributeSummaryItem> summarize(AdminAlcoholAttributeSummaryRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAttributedAlcoholSearchRequest request);

  Page<AdminAttributedAlcoholItem> searchAlcohols(AdminAlcoholCompoundSearchRequest request);

  AdminAlcoholAttributeItem create(AdminAlcoholAttributeCreateRequest request);

  AdminAlcoholAttributeBulkResponse upsertBulk(AdminAlcoholAttributeBulkRequest request);

  AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request);

  AdminResultResponse delete(Long id);

  List<AdminAlcoholAttributeItem> deleteBatch(AdminAlcoholAttributeBatchDeleteRequest request);

  List<AlcoholAttributeKeyCountItem> findKeys();

  List<AlcoholAttributeKeywordCountItem> findKeywords();

  AdminAlcoholAttributeEnumsResponse findEnums();

  AdminAlcoholAttributeMappingBulkResponse mapBulk(AdminAlcoholAttributeMappingBulkRequest request);

  Page<AdminAlcoholAttributeMappingItem> findMappings(
      AdminAlcoholAttributeMappingSearchRequest request);

  AdminAlcoholAttributeMappingItem updateMapping(
      Long id, AdminAlcoholAttributeMappingUpdateRequest request);

  AdminResultResponse deleteMapping(Long id);
}
