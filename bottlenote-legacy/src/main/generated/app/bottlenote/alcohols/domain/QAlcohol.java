package app.bottlenote.alcohols.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlcohol is a Querydsl query type for Alcohol
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlcohol extends EntityPathBase<Alcohol> {

    private static final long serialVersionUID = -1337571484L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAlcohol alcohol = new QAlcohol("alcohol");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final StringPath abv = createString("abv");

    public final StringPath age = createString("age");

    public final SetPath<AlcoholsTastingTags, QAlcoholsTastingTags> alcoholsTastingTags = this.<AlcoholsTastingTags, QAlcoholsTastingTags>createSet("alcoholsTastingTags", AlcoholsTastingTags.class, QAlcoholsTastingTags.class, PathInits.DIRECT2);

    public final StringPath cask = createString("cask");

    public final EnumPath<app.bottlenote.alcohols.constant.AlcoholCategoryGroup> categoryGroup = createEnum("categoryGroup", app.bottlenote.alcohols.constant.AlcoholCategoryGroup.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final StringPath description = createString("description");

    public final QDistillery distillery;

    public final StringPath engCategory = createString("engCategory");

    public final StringPath engName = createString("engName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath korCategory = createString("korCategory");

    public final StringPath korName = createString("korName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final QRegion region;

    public final EnumPath<app.bottlenote.alcohols.constant.AlcoholType> type = createEnum("type", app.bottlenote.alcohols.constant.AlcoholType.class);

    public final StringPath volume = createString("volume");

    public QAlcohol(String variable) {
        this(Alcohol.class, forVariable(variable), INITS);
    }

    public QAlcohol(Path<? extends Alcohol> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAlcohol(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAlcohol(PathMetadata metadata, PathInits inits) {
        this(Alcohol.class, metadata, inits);
    }

    public QAlcohol(Class<? extends Alcohol> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.distillery = inits.isInitialized("distillery") ? new QDistillery(forProperty("distillery")) : null;
        this.region = inits.isInitialized("region") ? new QRegion(forProperty("region")) : null;
    }

}

