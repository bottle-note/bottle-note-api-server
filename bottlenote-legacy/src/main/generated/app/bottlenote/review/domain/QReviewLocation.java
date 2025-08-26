package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReviewLocation is a Querydsl query type for ReviewLocation
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QReviewLocation extends BeanPath<ReviewLocation> {

    private static final long serialVersionUID = 798556234L;

    public static final QReviewLocation reviewLocation = new QReviewLocation("reviewLocation");

    public final StringPath address = createString("address");

    public final StringPath category = createString("category");

    public final StringPath detailAddress = createString("detailAddress");

    public final StringPath latitude = createString("latitude");

    public final StringPath longitude = createString("longitude");

    public final StringPath mapUrl = createString("mapUrl");

    public final StringPath name = createString("name");

    public final StringPath zipCode = createString("zipCode");

    public QReviewLocation(String variable) {
        super(ReviewLocation.class, forVariable(variable));
    }

    public QReviewLocation(Path<? extends ReviewLocation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewLocation(PathMetadata metadata) {
        super(ReviewLocation.class, metadata);
    }

}

