package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * MFDS 엔티티 테스트 팩토리
 *
 * <p>수집기 적재를 흉내 내 MFDS 엔티티를 영속화하는 헬퍼 클래스. 생성 API가 없는 필드는 {@link MfdsTestData}의 리플렉션 빌더로 채운다.
 */
@Component
public class MfdsTestFactory {

  @PersistenceContext private EntityManager em;

  /** 수집기 적재를 재현하며 원본 데이터도 채워 응답 제외 여부를 검증한다. */
  @Transactional
  public Long persistItem(
      String rcno, String productNameKo, LocalDate processedDate, LocalDateTime observedAt) {
    em.createNativeQuery(
            """
            INSERT INTO mfds_jobs
              (job_type, requested_from_date, requested_to_date, status, config_json)
            VALUES ('TEST', '2026-09-01', '2026-09-01', 'COMPLETED', '{}')
            """)
        .executeUpdate();
    Long jobId = lastInsertedId();
    em.createNativeQuery(
            """
            INSERT INTO mfds_tasks (job_id, process_date, status)
            VALUES (:jobId, '2026-09-01', 'COMPLETED')
            """)
        .setParameter("jobId", jobId)
        .executeUpdate();
    Long taskId = lastInsertedId();
    em.createNativeQuery(
            """
            INSERT INTO mfds_fetches
              (job_id, task_id, item_code, item_name, page_no, request_key_sha256,
               request_method, request_url, request_query_json, attempt_no, started_at, status)
            VALUES (:jobId, :taskId, 'WHISKY', '위스키', 1, UNHEX(SHA2('test', 256)),
                    'GET', 'https://example.test/mfds', '{}', 1, :observedAt, 'COMPLETED')
            """)
        .setParameter("jobId", jobId)
        .setParameter("taskId", taskId)
        .setParameter("observedAt", observedAt)
        .executeUpdate();
    Long fetchId = lastInsertedId();
    em.createNativeQuery(
            """
            INSERT INTO mfds_items
              (job_id, task_id, fetch_id, row_no, rcno, queried_item_code, queried_item_name,
               product_division_name, importer_name, product_name_ko, product_name_en, item_name,
               overseas_establishment_name, processed_date_raw, processed_date, expiry_text,
               manufacture_country_name, export_country_name, detail_href, canonical_values_json,
               raw_row_html, raw_row_sha256, semantic_sha256, parser_version, observed_at)
            VALUES (:jobId, :taskId, :fetchId, 1, :rcno, 'WHISKY', '위스키',
                    '가공식품', '보틀상사', :productNameKo, 'GLENFIDDICH 12 700ML', '위스키',
                    '테스트 제조업소', '2026.09.01', :processedDate, '해당없음',
                    '영국', '영국', '/detail/test', '{"test":"raw"}',
                    '<tr>raw</tr>', UNHEX(SHA2('raw', 256)), UNHEX(SHA2('semantic', 256)),
                    'test', :observedAt)
            """)
        .setParameter("jobId", jobId)
        .setParameter("taskId", taskId)
        .setParameter("fetchId", fetchId)
        .setParameter("rcno", rcno)
        .setParameter("productNameKo", productNameKo)
        .setParameter("processedDate", processedDate)
        .setParameter("observedAt", observedAt)
        .executeUpdate();
    return lastInsertedId();
  }

  private Long lastInsertedId() {
    return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
  }

  /** 기본 수입사 생성 */
  @Transactional
  @NotNull
  public MfdsImporter persistImporter(
      @NotNull String officialBusinessCode,
      @NotNull String businessName,
      @NotNull MfdsImporterAdminStatus adminStatus) {
    MfdsImporter importer = MfdsTestData.importer(officialBusinessCode, businessName, adminStatus);
    em.persist(importer);
    em.flush();
    return importer;
  }

  /** 정제 신고 데이터 생성. 매칭·연결 필드는 필요한 값만 지정한다. */
  @Transactional
  @NotNull
  public MfdsDeclaration persistDeclaration(
      @NotNull String rcno,
      @NotNull MfdsNormalizationStatus normalizationStatus,
      @Nullable Long importerId,
      @Nullable Long selectedAlcoholId,
      @Nullable String alcoholMatchDecision) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, normalizationStatus, importerId, selectedAlcoholId, alcoholMatchDecision, null, null);
    if (importerId != null) {
      MfdsTestData.set(declaration, "importerLinkSource", MfdsImporterLinkSource.PAGE_NAME);
    }
    em.persist(declaration);
    em.flush();
    return declaration;
  }

  /** Product 공개 조회 테스트용. 한글명·처리일자·수출국을 영속 전에 채운다. */
  @Transactional
  @NotNull
  public MfdsDeclaration persistPublicDeclaration(
      @NotNull String rcno,
      @Nullable Long importerId,
      @Nullable String importerBaseName,
      @NotNull String alcoholNameKo,
      @Nullable LocalDate processedDate,
      @Nullable String exportCountryAlpha2,
      @Nullable String exportCountryNameKo) {
    return persistPublicDeclarationWithCategory(
        rcno,
        importerId,
        importerBaseName,
        alcoholNameKo,
        processedDate,
        exportCountryAlpha2,
        exportCountryNameKo,
        null,
        null);
  }

  /** Product 공개 조회 테스트용. 카테고리 ko/en까지 영속 전에 채운다. */
  @Transactional
  @NotNull
  public MfdsDeclaration persistPublicDeclarationWithCategory(
      @NotNull String rcno,
      @Nullable Long importerId,
      @Nullable String importerBaseName,
      @NotNull String alcoholNameKo,
      @Nullable LocalDate processedDate,
      @Nullable String exportCountryAlpha2,
      @Nullable String exportCountryNameKo,
      @Nullable String alcoholCategoryKo,
      @Nullable String alcoholCategoryEn) {
    MfdsDeclaration declaration =
        MfdsTestData.publicDeclaration(
            rcno,
            importerId,
            importerBaseName,
            alcoholNameKo,
            processedDate,
            exportCountryAlpha2,
            exportCountryNameKo);
    if (alcoholCategoryKo != null) {
      MfdsTestData.set(declaration, "alcoholCategoryKo", alcoholCategoryKo);
    }
    if (alcoholCategoryEn != null) {
      MfdsTestData.set(declaration, "alcoholCategoryEn", alcoholCategoryEn);
    }
    em.persist(declaration);
    em.flush();
    return declaration;
  }

  /** RCNO별 수입사 연결 근거 생성 */
  @Transactional
  @NotNull
  public MfdsImporterRcnoLink persistRcnoLink(
      @NotNull String rcno, @NotNull Long importerId, @NotNull String sourceImporterName) {
    MfdsImporterRcnoLink link =
        MfdsTestData.rcnoLink(rcno, importerId, sourceImporterName, MfdsImporterLinkSource.PAGE_RCNO);
    em.persist(link);
    em.flush();
    return link;
  }
}
