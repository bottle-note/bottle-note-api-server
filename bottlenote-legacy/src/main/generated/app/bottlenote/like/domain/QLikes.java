package app.bottlenote.like.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLikes is a Querydsl query type for Likes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLikes extends EntityPathBase<Likes> {

    private static final long serialVersionUID = 41716192L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLikes likes = new QLikes("likes");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final NumberPath<Long> reviewId = createNumber("reviewId", Long.class);

    public final EnumPath<app.bottlenote.like.constant.LikeStatus> status = createEnum("status", app.bottlenote.like.constant.LikeStatus.class);

    public final QLikeUserInfo userInfo;

    public QLikes(String variable) {
        this(Likes.class, forVariable(variable), INITS);
    }

    public QLikes(Path<? extends Likes> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLikes(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLikes(PathMetadata metadata, PathInits inits) {
        this(Likes.class, metadata, inits);
    }

    public QLikes(Class<? extends Likes> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.userInfo = inits.isInitialized("userInfo") ? new QLikeUserInfo(forProperty("userInfo")) : null;
    }

}

