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

/** 중복 주류 병합 로그. 중복 주류 한 건당 한 행이며 만든 뒤에는 바꾸지 않는다. */
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
  @Column(name = "target_alcohol_id", nullable = false)
  private Long targetAlcoholId;

  @Comment("병합 시점의 기준 주류 한글 이름")
  @Column(name = "target_kor_name", nullable = false)
  private String targetKorName;

  @Comment("병합되어 삭제 처리된 중복 주류 ID")
  @Column(name = "source_alcohol_id", nullable = false)
  private Long sourceAlcoholId;

  @Comment("병합 시점의 중복 주류 한글 이름")
  @Column(name = "source_kor_name", nullable = false)
  private String sourceKorName;

  @Comment("병합 시점의 중복 주류 영문 이름")
  @Column(name = "source_eng_name", nullable = false)
  private String sourceEngName;

  @Comment("대상별 이전·삭제 건수")
  @Type(JsonType.class)
  @Column(name = "counts", nullable = false, columnDefinition = "json")
  private List<AlcoholMergeCount> counts;

  public static AlcoholMergeLog of(
      String mergeGroupId, Alcohol target, Alcohol source, List<AlcoholMergeCount> counts) {
    AlcoholMergeLog log = new AlcoholMergeLog();
    log.mergeGroupId = mergeGroupId;
    log.targetAlcoholId = target.getId();
    log.targetKorName = target.getKorName();
    log.sourceAlcoholId = source.getId();
    log.sourceKorName = source.getKorName();
    log.sourceEngName = source.getEngName();
    log.counts = counts;
    return log;
  }
}
