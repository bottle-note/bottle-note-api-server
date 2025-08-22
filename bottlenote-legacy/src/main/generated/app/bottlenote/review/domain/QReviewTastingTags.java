package app.bottlenote.review.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewTastingTags is a Querydsl query type for ReviewTastingTags
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QReviewTastingTags extends BeanPath<ReviewTastingTags> {

    private static final long serialVersionUID = 87025944L;

    public static final QReviewTastingTags reviewTastingTags = new QReviewTastingTags("reviewTastingTags");

    public final SetPath<ReviewTastingTag, QReviewTastingTag> reviewTastingTagSet = this.<ReviewTastingTag, QReviewTastingTag>createSet("reviewTastingTagSet", ReviewTastingTag.class, QReviewTastingTag.class, PathInits.DIRECT2);

    public QReviewTastingTags(String variable) {
        super(ReviewTastingTags.class, forVariable(variable));
    }

    public QReviewTastingTags(Path<? extends ReviewTastingTags> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewTastingTags(PathMetadata metadata) {
        super(ReviewTastingTags.class, metadata);
    }

}

