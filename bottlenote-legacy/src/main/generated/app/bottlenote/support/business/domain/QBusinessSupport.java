package app.bottlenote.support.business.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBusinessSupport is a Querydsl query type for BusinessSupport
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBusinessSupport extends EntityPathBase<BusinessSupport> {

    private static final long serialVersionUID = -1664991701L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBusinessSupport businessSupport = new QBusinessSupport("businessSupport");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    public final QBusinessImageList businessImageList;

    public final EnumPath<app.bottlenote.support.business.constant.BusinessSupportType> businessSupportType = createEnum("businessSupportType", app.bottlenote.support.business.constant.BusinessSupportType.class);

    public final StringPath contact = createString("contact");

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final StringPath responseContent = createString("responseContent");

    public final EnumPath<app.bottlenote.support.constant.StatusType> status = createEnum("status", app.bottlenote.support.constant.StatusType.class);

    public final StringPath title = createString("title");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QBusinessSupport(String variable) {
        this(BusinessSupport.class, forVariable(variable), INITS);
    }

    public QBusinessSupport(Path<? extends BusinessSupport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBusinessSupport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBusinessSupport(PathMetadata metadata, PathInits inits) {
        this(BusinessSupport.class, metadata, inits);
    }

    public QBusinessSupport(Class<? extends BusinessSupport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.businessImageList = inits.isInitialized("businessImageList") ? new QBusinessImageList(forProperty("businessImageList")) : null;
    }

}

