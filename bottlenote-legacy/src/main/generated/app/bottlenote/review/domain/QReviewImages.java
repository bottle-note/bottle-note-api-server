package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewImages is a Querydsl query type for ReviewImages
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QReviewImages extends BeanPath<ReviewImages> {

    private static final long serialVersionUID = 641533005L;

    public static final QReviewImages reviewImages = new QReviewImages("reviewImages");

    public final ListPath<ReviewImage, QReviewImage> images = this.<ReviewImage, QReviewImage>createList("images", ReviewImage.class, QReviewImage.class, PathInits.DIRECT2);

    public QReviewImages(String variable) {
        super(ReviewImages.class, forVariable(variable));
    }

    public QReviewImages(Path<? extends ReviewImages> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewImages(PathMetadata metadata) {
        super(ReviewImages.class, metadata);
    }

}

