package app.bottlenote.support.help.repository.custom;

import static app.bottlenote.support.help.domain.QHelp.help;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.repository.HelpQuerySupporter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomHelpQueryRepositoryImpl implements CustomHelpQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final HelpQuerySupporter supporter;

  @Override
  public PageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId) {

    List<HelpListResponse.HelpInfo> fetch =
        queryFactory
            .select(supporter.helpResponseConstructor())
            .from(help)
            .where(help.userId.eq(currentUserId))
            .offset(helpPageableRequest.cursor())
            .limit(helpPageableRequest.pageSize() + 1)
            .fetch();

    Long totalCount =
        queryFactory
            .select(help.id.count())
            .from(help)
            .where(help.userId.eq(currentUserId))
            .fetchOne();

    CursorPageable cursorPageable = getCursorPageable(helpPageableRequest, fetch);
    log.info("CURSOR Pageable info :{}", cursorPageable.toString());

    return PageResponse.of(HelpListResponse.of(totalCount, fetch), cursorPageable);
  }

  private CursorPageable getCursorPageable(
      HelpPageableRequest helpPageableRequest, List<HelpListResponse.HelpInfo> fetch) {

    boolean hasNext = isHasNext(helpPageableRequest, fetch);
    return CursorPageable.builder()
        .cursor(helpPageableRequest.cursor() + helpPageableRequest.pageSize())
        .pageSize(helpPageableRequest.pageSize())
        .hasNext(hasNext)
        .currentCursor(helpPageableRequest.cursor())
        .build();
  }

  /** 다음 페이지가 있는지 확인하는 메소드 */
  private boolean isHasNext(
      HelpPageableRequest helpPageableRequest, List<HelpListResponse.HelpInfo> fetch) {
    boolean hasNext = fetch.size() > helpPageableRequest.pageSize();

    if (hasNext) {
      fetch.remove(fetch.size() - 1); // Remove the extra record
    }
    return hasNext;
  }

  @Override
  public PageResponse<AdminHelpListResponse> getAdminHelpList(AdminHelpPageableRequest request) {
    BooleanBuilder whereClause = new BooleanBuilder();

    if (request.status() != null) {
      whereClause.and(help.status.eq(request.status()));
    }
    if (request.type() != null) {
      whereClause.and(help.type.eq(request.type()));
    }

    List<AdminHelpListResponse.AdminHelpInfo> fetch =
        queryFactory
            .select(supporter.adminHelpResponseConstructor())
            .from(help)
            .leftJoin(user)
            .on(help.userId.eq(user.id))
            .where(whereClause)
            .orderBy(help.createAt.desc())
            .offset(request.cursor())
            .limit(request.pageSize() + 1)
            .fetch();

    Long totalCount = queryFactory.select(help.id.count()).from(help).where(whereClause).fetchOne();

    CursorPageable cursorPageable = getAdminCursorPageable(request, fetch);
    log.info("Admin CURSOR Pageable info: {}", cursorPageable.toString());

    return PageResponse.of(AdminHelpListResponse.of(totalCount, fetch), cursorPageable);
  }

  private CursorPageable getAdminCursorPageable(
      AdminHelpPageableRequest request, List<AdminHelpListResponse.AdminHelpInfo> fetch) {

    boolean hasNext = fetch.size() > request.pageSize();
    if (hasNext) {
      fetch.remove(fetch.size() - 1);
    }

    return CursorPageable.builder()
        .cursor(request.cursor() + request.pageSize())
        .pageSize(request.pageSize())
        .hasNext(hasNext)
        .currentCursor(request.cursor())
        .build();
  }
}
