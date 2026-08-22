package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * MFDS 엔티티 단위 테스트 데이터 생성기.
 *
 * <p>MFDS 엔티티는 외부 수집기가 적재하는 읽기 전용 모델이라 생성 API가 없다. 테스트에서는 리플렉션으로 인스턴스를 만들고 검색에 필요한 필드만 채운다.
 */
public final class MfdsTestData {

  private MfdsTestData() {}

  public static MfdsImporter importer(
      String officialBusinessCode, String businessName, MfdsImporterAdminStatus adminStatus) {
    MfdsImporter importer = instantiate(MfdsImporter.class);
    set(importer, "officialBusinessCode", officialBusinessCode);
    set(importer, "licenseNo", "제0000호-" + officialBusinessCode);
    set(importer, "businessName", businessName);
    set(importer, "businessNameKeySha256", new byte[32]);
    set(importer, "operatingStatus", "영업");
    set(importer, "sourceListUrl", "https://example.mfds.go.kr/list");
    set(importer, "sourceListSha256", new byte[32]);
    set(importer, "sourceObservedAt", LocalDateTime.of(2026, 8, 1, 0, 0));
    set(importer, "adminStatus", adminStatus);
    return importer;
  }

  public static MfdsDeclaration declaration(
      String rcno,
      MfdsNormalizationStatus normalizationStatus,
      Long importerId,
      Long selectedAlcoholId,
      String alcoholMatchDecision,
      String nameSearchKeyKo,
      String nameSearchKeyEn) {
    MfdsDeclaration declaration = instantiate(MfdsDeclaration.class);
    set(declaration, "rcno", rcno);
    set(declaration, "sourceItemId", 1L);
    set(declaration, "normalizationStatus", normalizationStatus);
    set(declaration, "importerId", importerId);
    set(declaration, "selectedAlcoholId", selectedAlcoholId);
    set(declaration, "alcoholMatchDecision", alcoholMatchDecision);
    set(declaration, "nameSearchKeyKo", nameSearchKeyKo);
    set(declaration, "nameSearchKeyEn", nameSearchKeyEn);
    set(declaration, "reviewStatus", "PENDING");
    return declaration;
  }

  public static MfdsImporterRcnoLink rcnoLink(
      String rcno, Long importerId, String sourceImporterName, MfdsImporterLinkSource linkSource) {
    MfdsImporterRcnoLink link = instantiate(MfdsImporterRcnoLink.class);
    set(link, "rcno", rcno);
    set(link, "importerId", importerId);
    set(link, "sourceImporterName", sourceImporterName);
    set(link, "linkSource", linkSource);
    return link;
  }

  public static void set(Object target, String field, Object value) {
    ReflectionTestUtils.setField(target, field, value);
  }

  private static <T> T instantiate(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(type.getSimpleName() + " 인스턴스 생성 실패", exception);
    }
  }
}
