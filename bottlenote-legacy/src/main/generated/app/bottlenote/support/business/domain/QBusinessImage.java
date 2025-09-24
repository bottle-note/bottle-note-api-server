package app.bottlenote.support.business.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBusinessImage is a Querydsl query type for BusinessImage
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBusinessImage extends EntityPathBase<BusinessImage> {

    private static final long serialVersionUID = -341946697L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBusinessImage businessImage = new QBusinessImage("businessImage");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

    public final app.bottlenote.common.image.QImageInfo businessImageInfo;

    public final NumberPath<Long> businessSupportId = createNumber("businessSupportId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public QBusinessImage(String variable) {
        this(BusinessImage.class, forVariable(variable), INITS);
    }

    public QBusinessImage(Path<? extends BusinessImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBusinessImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBusinessImage(PathMetadata metadata, PathInits inits) {
        this(BusinessImage.class, metadata, inits);
    }

    public QBusinessImage(Class<? extends BusinessImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.businessImageInfo = inits.isInitialized("businessImageInfo") ? new app.bottlenote.common.image.QImageInfo(forProperty("businessImageInfo")) : null;
    }

}

