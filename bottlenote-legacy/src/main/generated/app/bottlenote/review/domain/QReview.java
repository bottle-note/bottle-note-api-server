package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReview is a Querydsl query type for Review
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReview extends EntityPathBase<Review> {

    private static final long serialVersionUID = -1611054731L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReview review = new QReview("review");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final EnumPath<app.bottlenote.review.constant.ReviewActiveStatus> activeStatus = createEnum("activeStatus", app.bottlenote.review.constant.ReviewActiveStatus.class);

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final BooleanPath isBest = createBoolean("isBest");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final QReviewImages reviewImages;

    public final QReviewLocation reviewLocation;

    public final NumberPath<Double> reviewRating = createNumber("reviewRating", Double.class);

    public final QReviewTastingTags reviewTastingTags;

    public final EnumPath<app.bottlenote.review.constant.SizeType> sizeType = createEnum("sizeType", app.bottlenote.review.constant.SizeType.class);

    public final EnumPath<app.bottlenote.review.constant.ReviewDisplayStatus> status = createEnum("status", app.bottlenote.review.constant.ReviewDisplayStatus.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Long> viewCount = createNumber("viewCount", Long.class);

    public QReview(String variable) {
        this(Review.class, forVariable(variable), INITS);
    }

    public QReview(Path<? extends Review> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReview(PathMetadata metadata, PathInits inits) {
        this(Review.class, metadata, inits);
    }

    public QReview(Class<? extends Review> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reviewImages = inits.isInitialized("reviewImages") ? new QReviewImages(forProperty("reviewImages")) : null;
        this.reviewLocation = inits.isInitialized("reviewLocation") ? new QReviewLocation(forProperty("reviewLocation")) : null;
        this.reviewTastingTags = inits.isInitialized("reviewTastingTags") ? new QReviewTastingTags(forProperty("reviewTastingTags")) : null;
    }

}

