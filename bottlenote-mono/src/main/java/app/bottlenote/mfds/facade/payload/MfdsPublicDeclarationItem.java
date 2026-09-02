package app.bottlenote.mfds.facade.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.lang.Nullable;

/**
 * Product 공개용 수입 신고 정보.
 *
 * <p>필드명과 importer 중첩은 Admin 상세의 공개 가능 부분집합과 일치한다. 매칭 점수·사유·검토 메모·원문·내부 운영
 * 상태는 포함하지 않는다. {@code importer}는 연결이 없으면 null이다.
 */
public record MfdsPublicDeclarationItem(
    Long id,
    String rcno,
    String baseProductNameKo,
    String baseProductNameEn,
    String skuDisplayNameKo,
    String skuDisplayNameEn,
    Integer volumeMl,
    Integer unitVolumeMl,
    Integer packageCount,
    BigDecimal abvPercent,
    Short ageYears,
    Short vintageYear,
    String editionName,
    String caskNumber,
    String batchNumber,
    LocalDate expiryStart,
    LocalDate expiryEnd,
    String importerBaseName,
    String manufacturerName,
    String alcoholNameKo,
    String alcoholNameEn,
    String alcoholCategoryKo,
    String alcoholCategoryEn,
    String manufactureCountryNameKo,
    String exportCountryNameKo,
    @Schema(nullable = true) @Nullable MfdsPublicImporterItem importer) {}
