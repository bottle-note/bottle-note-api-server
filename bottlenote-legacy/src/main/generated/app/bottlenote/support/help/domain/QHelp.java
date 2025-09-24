package app.bottlenote.support.help.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHelp is a Querydsl query type for Help
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHelp extends EntityPathBase<Help> {

    private static final long serialVersionUID = -1163589498L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHelp help = new QHelp("help");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final QHelpImageList helpImageList;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final StringPath responseContent = createString("responseContent");

    public final EnumPath<app.bottlenote.support.constant.StatusType> status = createEnum("status", app.bottlenote.support.constant.StatusType.class);

    public final StringPath title = createString("title");

    public final EnumPath<app.bottlenote.support.help.constant.HelpType> type = createEnum("type", app.bottlenote.support.help.constant.HelpType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QHelp(String variable) {
        this(Help.class, forVariable(variable), INITS);
    }

    public QHelp(Path<? extends Help> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHelp(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHelp(PathMetadata metadata, PathInits inits) {
        this(Help.class, metadata, inits);
    }

    public QHelp(Class<? extends Help> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.helpImageList = inits.isInitialized("helpImageList") ? new QHelpImageList(forProperty("helpImageList")) : null;
    }

}

