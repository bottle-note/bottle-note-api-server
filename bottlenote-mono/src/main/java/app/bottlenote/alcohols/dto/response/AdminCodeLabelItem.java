package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.CodeLabel;
import java.util.Arrays;
import java.util.List;

/** enum 값을 화면 표기와 함께 내려주는 공용 항목. 서버는 code만 관리하고 한글 표기는 enum 필드에서 가져온다. */
public record AdminCodeLabelItem(String code, String korName, String engName, String description) {

  public static AdminCodeLabelItem of(CodeLabel value) {
    return new AdminCodeLabelItem(
        value.name(), value.getKorName(), value.getEngName(), value.getDescription());
  }

  public static List<AdminCodeLabelItem> listOf(CodeLabel[] values) {
    return Arrays.stream(values).map(AdminCodeLabelItem::of).toList();
  }
}
