package app.bottlenote.mfds.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MFDS 엔티티 매핑 계약")
class MfdsEntityMappingContractTest {

  @Test
  @DisplayName("관리 대상 MFDS 테이블 세 개를 명시적으로 매핑한다")
  void 관리_대상_MFDS_테이블을_매핑한다() {
    assertEntity(MfdsImporter.class, "mfds_importer", "mfds_importers");
    assertEntity(MfdsDeclaration.class, "mfds_declaration", "mfds_declarations");
    assertEntity(
        MfdsImporterRcnoLink.class,
        "mfds_importer_rcno_link",
        "mfds_importer_rcno_links");
  }

  @Test
  @DisplayName("수입사와 신고 원장의 관리 및 노출 필드를 매핑한다")
  void 관리와_노출_필드를_매핑한다() throws NoSuchFieldException {
    assertColumn(MfdsImporter.class, "officialBusinessCode", "official_business_code");
    assertColumn(MfdsImporter.class, "businessName", "business_name");
    assertRequiredBinaryColumn(
        MfdsImporter.class, "businessNameKeySha256", "business_name_key_sha256");
    assertRequiredBinaryColumn(MfdsImporter.class, "sourceListSha256", "source_list_sha256");
    assertColumn(MfdsImporter.class, "adminStatus", "admin_status");
    assertThat(
            MfdsImporter.class
                .getDeclaredField("adminStatus")
                .isAnnotationPresent(Enumerated.class))
        .isTrue();

    assertColumn(MfdsDeclaration.class, "rcno", "rcno");
    assertColumn(MfdsDeclaration.class, "importerId", "importer_id");
    assertColumn(MfdsDeclaration.class, "normalizationStatus", "normalization_status");
    assertColumn(MfdsDeclaration.class, "selectedAlcoholId", "selected_alcohol_id");
    assertThat(MfdsDeclaration.class.getDeclaredField("ageYears").getType())
        .isEqualTo(Short.class);
    assertThat(MfdsDeclaration.class.getDeclaredField("vintageYear").getType())
        .isEqualTo(Short.class);
    assertColumnDefinition(
        MfdsDeclaration.class, "manufactureCountryAlpha2", "CHAR(2)");
    assertColumnDefinition(
        MfdsDeclaration.class, "manufactureCountryAlpha3", "CHAR(3)");
    assertColumnDefinition(MfdsDeclaration.class, "exportCountryAlpha2", "CHAR(2)");
    assertColumnDefinition(MfdsDeclaration.class, "exportCountryAlpha3", "CHAR(3)");

    assertColumn(MfdsImporterRcnoLink.class, "rcno", "rcno");
    assertColumn(MfdsImporterRcnoLink.class, "importerId", "importer_id");
    assertColumn(MfdsImporterRcnoLink.class, "linkSource", "link_source");
    assertColumn(MfdsImporterRcnoLink.class, "sourceGallerySha256", "source_gallery_sha256");
  }

  @Test
  @DisplayName("수집기 내부 lease 상태는 Admin 관리 엔티티에서 제외한다")
  void 수집기_내부_상태는_제외한다() {
    assertThatThrownBy(() -> MfdsDeclaration.class.getDeclaredField("claimOwner"))
        .isInstanceOf(NoSuchFieldException.class);
    assertThatThrownBy(() -> MfdsDeclaration.class.getDeclaredField("claimLeaseUntil"))
        .isInstanceOf(NoSuchFieldException.class);
  }

  private static void assertEntity(Class<?> type, String entityName, String tableName) {
    assertThat(type.getAnnotation(Entity.class).name()).isEqualTo(entityName);
    assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName);
  }

  private static void assertColumn(Class<?> type, String fieldName, String columnName)
      throws NoSuchFieldException {
    Field field = type.getDeclaredField(fieldName);
    assertThat(field.getAnnotation(Column.class).name()).isEqualTo(columnName);
  }

  private static void assertColumnDefinition(
      Class<?> type, String fieldName, String columnDefinition) throws NoSuchFieldException {
    Column column = type.getDeclaredField(fieldName).getAnnotation(Column.class);
    assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
  }

  private static void assertRequiredBinaryColumn(
      Class<?> type, String fieldName, String columnName) throws NoSuchFieldException {
    Field field = type.getDeclaredField(fieldName);
    Column column = field.getAnnotation(Column.class);
    assertThat(field.getType()).isEqualTo(byte[].class);
    assertThat(column.name()).isEqualTo(columnName);
    assertThat(column.nullable()).isFalse();
    assertThat(column.columnDefinition()).isEqualTo("BINARY(32)");
  }
}
