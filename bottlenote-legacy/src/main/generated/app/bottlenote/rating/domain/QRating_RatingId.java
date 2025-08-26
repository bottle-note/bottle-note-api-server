package app.bottlenote.rating.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRating_RatingId is a Querydsl query type for RatingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRating_RatingId extends BeanPath<Rating.RatingId> {

    private static final long serialVersionUID = -1750058105L;

    public static final QRating_RatingId ratingId = new QRating_RatingId("ratingId");

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRating_RatingId(String variable) {
        super(Rating.RatingId.class, forVariable(variable));
    }

    public QRating_RatingId(Path<? extends Rating.RatingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRating_RatingId(PathMetadata metadata) {
        super(Rating.RatingId.class, metadata);
    }

}

