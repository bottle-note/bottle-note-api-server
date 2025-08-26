package app.bottlenote.alcohols.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPopularAlcohol is a Querydsl query type for PopularAlcohol
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPopularAlcohol extends EntityPathBase<PopularAlcohol> {

    private static final long serialVersionUID = -698275617L;

    public static final QPopularAlcohol popularAlcohol = new QPopularAlcohol("popularAlcohol");

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> day = createNumber("day", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    public final NumberPath<java.math.BigDecimal> pickScore = createNumber("pickScore", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> popularScore = createNumber("popularScore", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> ratingScore = createNumber("ratingScore", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> reviewScore = createNumber("reviewScore", java.math.BigDecimal.class);

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QPopularAlcohol(String variable) {
        super(PopularAlcohol.class, forVariable(variable));
    }

    public QPopularAlcohol(Path<? extends PopularAlcohol> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPopularAlcohol(PathMetadata metadata) {
        super(PopularAlcohol.class, metadata);
    }

}

