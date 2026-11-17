package app.bottlenote.support.help.repository;

import static app.bottlenote.support.help.domain.QHelp.help;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Component;

@Component
public class HelpQuerySupporter {

  /**
   * 문의글 목록 조회 API에 사용되는 생성자 Projection 메서드입니다. (사용자용)
   *
   * @return HelpInfo Projection
   */
  public ConstructorExpression<HelpListResponse.HelpInfo> helpResponseConstructor() {
    return Projections.constructor(
        HelpListResponse.HelpInfo.class,
        help.id.as("helpId"),
        help.title.as("title"),
        help.content.as("content"),
        help.createAt.as("createdAt"),
        help.status.as("helpStatus"));
  }

  /**
   * 문의글 목록 조회 API에 사용되는 생성자 Projection 메서드입니다. (관리자용)
   *
   * @return AdminHelpInfo Projection
   */
  public ConstructorExpression<AdminHelpListResponse.AdminHelpInfo> adminHelpResponseConstructor() {
    return Projections.constructor(
        AdminHelpListResponse.AdminHelpInfo.class,
        help.id.as("helpId"),
        help.userId.as("userId"),
        user.nickName.as("userNickname"),
        help.title.as("title"),
        help.type.as("type"),
        help.status.as("status"),
        help.createAt.as("createAt"));
  }
}
