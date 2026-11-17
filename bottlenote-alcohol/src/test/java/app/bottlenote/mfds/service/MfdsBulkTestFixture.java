package app.bottlenote.mfds.service;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.fixture.FakeAlcoholMatchTargetFacade;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingSelectionRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

/** 일괄 매칭 단위 테스트가 함께 쓰는 인메모리 구성. 단건 확정 서비스도 실제 구현을 쓴다. */
final class MfdsBulkTestFixture {

  static final long ADMIN_ID = 42L;
  static final Clock BASE_CLOCK =
      Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC);

  final InMemoryMfdsDeclarationRepository declarations;
  final InMemoryMfdsMatchingSelectionRepository selections =
      new InMemoryMfdsMatchingSelectionRepository();
  final InMemoryMfdsMatchingRepository matching = new InMemoryMfdsMatchingRepository();
  final FakeAlcoholMatchTargetFacade alcohols = new FakeAlcoholMatchTargetFacade();

  MfdsBulkTestFixture() {
    this(new InMemoryMfdsDeclarationRepository());
  }

  MfdsBulkTestFixture(InMemoryMfdsDeclarationRepository declarations) {
    this.declarations = declarations;
  }

  MfdsBulkMatchingService service(Clock clock) {
    return new MfdsBulkMatchingService(declarations, selections, single(), clock);
  }

  MfdsMatchingService single() {
    return new MfdsMatchingService(
        declarations,
        alcohols,
        new MfdsMatchingScoreCalculator(),
        selections,
        new MfdsMatchingHistoryService(
            matching, new MfdsMatchingEvidenceCodec(new ObjectMapper())));
  }

  MfdsDeclaration row(String rcno, byte[] identityKey) {
    return declarations.save(declaration(rcno, identityKey));
  }

  static MfdsDeclaration declaration(String rcno, byte[] identityKey) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", identityKey);
    return declaration;
  }

  static MfdsBulkMatchingPreviewResponse preview(
      MfdsBulkMatchingService service,
      Long sourceId,
      long alcoholId,
      Long distilleryId,
      Long regionId) {
    return service.preview(
        sourceId, new MfdsBulkMatchingPreviewRequest(alcoholId, distilleryId, regionId));
  }

  static Optional<MfdsBulkMatchingPreviewItem> item(
      MfdsBulkMatchingPreviewResponse preview, Long id) {
    return preview.items().stream().filter(item -> id.equals(item.declarationId())).findFirst();
  }

  static byte[] key(int marker) {
    byte[] value = new byte[32];
    value[31] = (byte) marker;
    return value;
  }

  static AlcoholMatchTargetItem alcohol(
      long id, String korName, String engName, Long distilleryId, Long regionId) {
    return new AlcoholMatchTargetItem(
        id,
        korName,
        engName,
        null,
        null,
        null,
        null,
        regionId,
        null,
        null,
        distilleryId,
        null,
        null,
        null,
        null);
  }
}
