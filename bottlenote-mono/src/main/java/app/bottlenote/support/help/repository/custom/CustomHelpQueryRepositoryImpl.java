package app.bottlenote.support.help.repository.custom;

import static app.bottlenote.support.help.domain.QHelp.help;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.repository.HelpQuerySupporter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomHelpQueryRepositoryImpl implements CustomHelpQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final HelpQuerySupporter supporter;
  private final HmacCursorCodec cursorCodec;

  @Override
  public app.bottlenote.global.pagination.PageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId) {
    String context = "help.list:" + currentUserId;
    int size = helpPageableRequest.size();
    BooleanExpression seek = helpSeek(helpPageableRequest.cursor(), context);

    List<HelpListResponse.HelpInfo> fetch =
        queryFactory
            .select(supporter.helpResponseConstructor())
            .from(help)
            .where(help.userId.eq(currentUserId), seek)
            .orderBy(help.createAt.desc(), help.id.desc())
            .limit(size + 1L)
            .fetch();

    Pagination.PageSlice<HelpListResponse.HelpInfo> slice =
        Pagination.fromOverflow(
            fetch,
            size,
            item -> cursorCodec.encode(context, TimeIdCursor.keys(item.createAt(), item.helpId())));
    return app.bottlenote.global.pagination.PageResponse.of(
        HelpListResponse.of(slice.items()), slice.pagination());
  }

  private BooleanExpression helpSeek(String cursor, String context) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    CursorClaims claims = cursorCodec.verify(cursor, context);
    LocalDateTime lastCreateAt = TimeIdCursor.time(claims);
    Long lastId = TimeIdCursor.id(claims);
    return help.createAt
        .lt(lastCreateAt)
        .or(help.createAt.eq(lastCreateAt).and(help.id.lt(lastId)));
  }

  @Override
  public app.bottlenote.global.pagination.PageResponse<AdminHelpListResponse> getAdminHelpList(
      AdminHelpPageableRequest request) {
    BooleanBuilder whereClause = new BooleanBuilder();
    String context = "admin.help:" + request.status() + ":" + request.type();
    int size = request.size();

    if (request.status() != null) {
      whereClause.and(help.status.eq(request.status()));
    }
    if (request.type() != null) {
      whereClause.and(help.type.eq(request.type()));
    }
    whereClause.and(helpSeek(request.cursor(), context));

    List<AdminHelpListResponse.AdminHelpInfo> fetch =
        queryFactory
            .select(supporter.adminHelpResponseConstructor())
            .from(help)
            .leftJoin(user)
            .on(help.userId.eq(user.id))
            .where(whereClause)
            .orderBy(help.createAt.desc(), help.id.desc())
            .limit(size + 1L)
            .fetch();

    Pagination.PageSlice<AdminHelpListResponse.AdminHelpInfo> slice =
        Pagination.fromOverflow(
            fetch,
            size,
            item -> cursorCodec.encode(context, TimeIdCursor.keys(item.createAt(), item.helpId())));
    return app.bottlenote.global.pagination.PageResponse.of(
        AdminHelpListResponse.of(slice.items()), slice.pagination());
  }
}
