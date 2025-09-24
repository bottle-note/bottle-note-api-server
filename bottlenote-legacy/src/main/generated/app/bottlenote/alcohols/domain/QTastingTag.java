package app.bottlenote.alcohols.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTastingTag is a Querydsl query type for TastingTag
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTastingTag extends EntityPathBase<TastingTag> {

    private static final long serialVersionUID = -1926748336L;

    public static final QTastingTag tastingTag = new QTastingTag("tastingTag");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

    public final ListPath<AlcoholsTastingTags, QAlcoholsTastingTags> alcoholsTastingTags = this.<AlcoholsTastingTags, QAlcoholsTastingTags>createList("alcoholsTastingTags", AlcoholsTastingTags.class, QAlcoholsTastingTags.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final StringPath description = createString("description");

    public final StringPath engName = createString("engName");

    public final StringPath icon = createString("icon");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath korName = createString("korName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public QTastingTag(String variable) {
        super(TastingTag.class, forVariable(variable));
    }

    public QTastingTag(Path<? extends TastingTag> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTastingTag(PathMetadata metadata) {
        super(TastingTag.class, metadata);
    }

}

