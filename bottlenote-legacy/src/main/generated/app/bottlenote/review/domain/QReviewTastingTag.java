package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewTastingTag is a Querydsl query type for ReviewTastingTag
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewTastingTag extends EntityPathBase<ReviewTastingTag> {

    private static final long serialVersionUID = -1936855365L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewTastingTag reviewTastingTag = new QReviewTastingTag("reviewTastingTag");

    public final app.bottlenote.common.domain.QBaseTimeEntity _super = new app.bottlenote.common.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    public final QReview review;

    public final StringPath tastingTag = createString("tastingTag");

    public QReviewTastingTag(String variable) {
        this(ReviewTastingTag.class, forVariable(variable), INITS);
    }

    public QReviewTastingTag(Path<? extends ReviewTastingTag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewTastingTag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewTastingTag(PathMetadata metadata, PathInits inits) {
        this(ReviewTastingTag.class, metadata, inits);
    }

    public QReviewTastingTag(Class<? extends ReviewTastingTag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.review = inits.isInitialized("review") ? new QReview(forProperty("review"), inits.get("review")) : null;
    }

}

