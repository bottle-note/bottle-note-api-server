package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewReply is a Querydsl query type for ReviewReply
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewReply extends EntityPathBase<ReviewReply> {

    private static final long serialVersionUID = 721519221L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewReply reviewReply = new QReviewReply("reviewReply");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

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

    public final QReviewReply parentReviewReply;

    public final ListPath<ReviewReply, QReviewReply> replies = this.<ReviewReply, QReviewReply>createList("replies", ReviewReply.class, QReviewReply.class, PathInits.DIRECT2);

    public final NumberPath<Long> reviewId = createNumber("reviewId", Long.class);

    public final QReviewReply rootReviewReply;

    public final EnumPath<app.bottlenote.review.constant.ReviewReplyStatus> status = createEnum("status", app.bottlenote.review.constant.ReviewReplyStatus.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QReviewReply(String variable) {
        this(ReviewReply.class, forVariable(variable), INITS);
    }

    public QReviewReply(Path<? extends ReviewReply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewReply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewReply(PathMetadata metadata, PathInits inits) {
        this(ReviewReply.class, metadata, inits);
    }

    public QReviewReply(Class<? extends ReviewReply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.parentReviewReply = inits.isInitialized("parentReviewReply") ? new QReviewReply(forProperty("parentReviewReply"), inits.get("parentReviewReply")) : null;
        this.rootReviewReply = inits.isInitialized("rootReviewReply") ? new QReviewReply(forProperty("rootReviewReply"), inits.get("rootReviewReply")) : null;
    }

}

