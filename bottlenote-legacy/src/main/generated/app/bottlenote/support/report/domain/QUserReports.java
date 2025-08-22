package app.bottlenote.support.report.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserReports is a Querydsl query type for UserReports
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserReports extends EntityPathBase<UserReports> {

    private static final long serialVersionUID = 643023804L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserReports userReports = new QUserReports("userReports");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final app.bottlenote.user.domain.QUser reportUser;

    public final StringPath responseContent = createString("responseContent");

    public final EnumPath<app.bottlenote.support.constant.StatusType> status = createEnum("status", app.bottlenote.support.constant.StatusType.class);

    public final EnumPath<app.bottlenote.support.report.constant.UserReportType> type = createEnum("type", app.bottlenote.support.report.constant.UserReportType.class);

    public final app.bottlenote.user.domain.QUser user;

    public QUserReports(String variable) {
        this(UserReports.class, forVariable(variable), INITS);
    }

    public QUserReports(Path<? extends UserReports> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserReports(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserReports(PathMetadata metadata, PathInits inits) {
        this(UserReports.class, metadata, inits);
    }

    public QUserReports(Class<? extends UserReports> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reportUser = inits.isInitialized("reportUser") ? new app.bottlenote.user.domain.QUser(forProperty("reportUser")) : null;
        this.user = inits.isInitialized("user") ? new app.bottlenote.user.domain.QUser(forProperty("user")) : null;
    }

}

