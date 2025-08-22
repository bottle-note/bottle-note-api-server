package app.bottlenote.alcohols.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDistillery is a Querydsl query type for Distillery
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDistillery extends EntityPathBase<Distillery> {

    private static final long serialVersionUID = -358947277L;

    public static final QDistillery distillery = new QDistillery("distillery");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final ListPath<Alcohol, QAlcohol> alcohol = this.<Alcohol, QAlcohol>createList("alcohol", Alcohol.class, QAlcohol.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final StringPath engName = createString("engName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath korName = createString("korName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final StringPath logoImgPath = createString("logoImgPath");

    public QDistillery(String variable) {
        super(Distillery.class, forVariable(variable));
    }

    public QDistillery(Path<? extends Distillery> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDistillery(PathMetadata metadata) {
        super(Distillery.class, metadata);
    }

}

