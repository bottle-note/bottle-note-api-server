package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import java.math.BigDecimal;
import java.util.List;

/**
 * 선택한 위스키들의 속성 묶음. 숫자형은 항목 단위, 그 외는 항목과 값 단위다.
 *
 * @param value 숫자가 아니거나 숫자인데 값이 모두 같으면 그 값. 숫자가 서로 다르면 null
 * @param registeredCount 묶음에 속한 속성 행 수
 * @param selectedCount 요청으로 선택한 위스키 수
 */
public record AdminAlcoholAttributeSummaryItem(
    String key,
    String value,
    AttributeType type,
    AttributeDatatype datatype,
    List<Long> alcoholIds,
    long registeredCount,
    long selectedCount,
    BigDecimal min,
    BigDecimal max,
    boolean allSameValue) {}
