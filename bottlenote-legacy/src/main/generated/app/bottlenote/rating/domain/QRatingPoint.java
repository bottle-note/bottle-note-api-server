package app.bottlenote.rating.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRatingPoint is a Querydsl query type for RatingPoint
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRatingPoint extends BeanPath<RatingPoint> {

    private static final long serialVersionUID = 1692367761L;

    public static final QRatingPoint ratingPoint = new QRatingPoint("ratingPoint");

    public final NumberPath<Double> rating = createNumber("rating", Double.class);

    public QRatingPoint(String variable) {
        super(RatingPoint.class, forVariable(variable));
    }

    public QRatingPoint(Path<? extends RatingPoint> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRatingPoint(PathMetadata metadata) {
        super(RatingPoint.class, metadata);
    }

}

