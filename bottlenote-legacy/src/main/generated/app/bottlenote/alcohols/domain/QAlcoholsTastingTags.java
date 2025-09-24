package app.bottlenote.alcohols.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlcoholsTastingTags is a Querydsl query type for AlcoholsTastingTags
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlcoholsTastingTags extends EntityPathBase<AlcoholsTastingTags> {

    private static final long serialVersionUID = -106245954L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAlcoholsTastingTags alcoholsTastingTags = new QAlcoholsTastingTags("alcoholsTastingTags");

    public final app.bottlenote.core.common.domain.QBaseTimeEntity _super = new app.bottlenote.core.common.domain.QBaseTimeEntity(this);

    public final QAlcohol alcohol;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    public final QTastingTag tastingTag;

    public QAlcoholsTastingTags(String variable) {
        this(AlcoholsTastingTags.class, forVariable(variable), INITS);
    }

    public QAlcoholsTastingTags(Path<? extends AlcoholsTastingTags> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAlcoholsTastingTags(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAlcoholsTastingTags(PathMetadata metadata, PathInits inits) {
        this(AlcoholsTastingTags.class, metadata, inits);
    }

    public QAlcoholsTastingTags(Class<? extends AlcoholsTastingTags> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.alcohol = inits.isInitialized("alcohol") ? new QAlcohol(forProperty("alcohol"), inits.get("alcohol")) : null;
        this.tastingTag = inits.isInitialized("tastingTag") ? new QTastingTag(forProperty("tastingTag")) : null;
    }

}

