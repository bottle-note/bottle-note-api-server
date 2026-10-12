package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;

/** 어드민 위스키 속성 조회·추가·수정·삭제. */
public interface AdminAlcoholAttributeService {

  List<AdminAlcoholAttributeItem> findAll(AdminAlcoholAttributeSearchRequest request);

  List<AdminAlcoholAttributeSummaryItem> summarize(AdminAlcoholAttributeSearchRequest request);

  AdminAlcoholAttributeCreateResponse create(AdminAlcoholAttributeCreateRequest request);

  AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request);

  AdminResultResponse delete(Long id);

  List<AdminAlcoholAttributeItem> deleteBatch(AdminAlcoholAttributeBatchDeleteRequest request);

  List<AlcoholAttributeKeyCountItem> findKeys();

  List<AlcoholAttributeKeywordCountItem> findKeywords();

  AdminAlcoholAttributeEnumsResponse findEnums();
}
