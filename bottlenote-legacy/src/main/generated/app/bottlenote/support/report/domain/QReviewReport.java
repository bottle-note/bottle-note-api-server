package app.bottlenote.support.report.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReviewReport is a Querydsl query type for ReviewReport
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewReport extends EntityPathBase<ReviewReport> {

    private static final long serialVersionUID = -1523724444L;

    public static final QReviewReport reviewReport = new QReviewReport("reviewReport");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ipAddress = createString("ipAddress");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final StringPath reportContent = createString("reportContent");

    public final StringPath responseContent = createString("responseContent");

    public final NumberPath<Long> reviewId = createNumber("reviewId", Long.class);

    public final EnumPath<app.bottlenote.support.constant.StatusType> status = createEnum("status", app.bottlenote.support.constant.StatusType.class);

    public final EnumPath<app.bottlenote.support.report.constant.ReviewReportType> type = createEnum("type", app.bottlenote.support.report.constant.ReviewReportType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QReviewReport(String variable) {
        super(ReviewReport.class, forVariable(variable));
    }

    public QReviewReport(Path<? extends ReviewReport> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewReport(PathMetadata metadata) {
        super(ReviewReport.class, metadata);
    }

}

