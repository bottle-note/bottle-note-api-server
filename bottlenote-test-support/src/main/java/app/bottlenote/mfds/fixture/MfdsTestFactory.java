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
    MfdsDeclaration declaration =
        MfdsTestData.publicDeclaration(
            rcno,
            importerId,
            importerBaseName,
            alcoholNameKo,
            processedDate,
            exportCountryAlpha2,
            exportCountryNameKo);
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
