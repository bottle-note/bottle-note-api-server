package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

/** 중복 위스키 병합 로그. 중복 위스키 한 건당 한 행이며 저장한 뒤에는 수정하지 않는다. */
@Getter
@Entity(name = "alcohol_merge_log")
@Table(name = "alcohol_merge_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholMergeLog extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("한 번의 병합 요청을 묶는 UUID")
  @Column(name = "merge_group_id", nullable = false, length = 36)
  private String mergeGroupId;

  @Comment("유지한 기준 주류 ID")
  @Column(name = "base_alcohol_id", nullable = false)
  private Long baseAlcoholId;

  @Comment("병합 시점의 기준 주류 한글 이름")
  @Column(name = "base_kor_name", nullable = false)
  private String baseKorName;

  @Comment("병합되어 삭제된 중복 주류 ID")
  @Column(name = "duplicate_alcohol_id", nullable = false)
  private Long duplicateAlcoholId;

  @Comment("병합 시점의 중복 주류 한글 이름")
  @Column(name = "duplicate_kor_name", nullable = false)
  private String duplicateKorName;

  @Comment("병합 시점의 중복 주류 영문 이름")
  @Column(name = "duplicate_eng_name", nullable = false)
  private String duplicateEngName;

  @Comment("대상별 이전·삭제 건수")
  @Type(JsonType.class)
  @Column(name = "counts", nullable = false, columnDefinition = "json")
  private List<AlcoholMergeCount> counts;

  public static AlcoholMergeLog of(
      String mergeGroupId, Alcohol base, Alcohol duplicate, List<AlcoholMergeCount> counts) {
    AlcoholMergeLog log = new AlcoholMergeLog();
    log.mergeGroupId = mergeGroupId;
    log.baseAlcoholId = base.getId();
    log.baseKorName = base.getKorName();
    log.duplicateAlcoholId = duplicate.getId();
    log.duplicateKorName = duplicate.getKorName();
    log.duplicateEngName = duplicate.getEngName();
    log.counts = counts;
    return log;
  }
}
