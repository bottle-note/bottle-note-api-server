package app.bottlenote.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFollow is a Querydsl query type for Follow
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFollow extends EntityPathBase<Follow> {

    private static final long serialVersionUID = -1716942015L;

    public static final QFollow follow = new QFollow("follow");

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

    public final EnumPath<app.bottlenote.user.constant.FollowStatus> status = createEnum("status", app.bottlenote.user.constant.FollowStatus.class);

    public final NumberPath<Long> targetUserId = createNumber("targetUserId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QFollow(String variable) {
        super(Follow.class, forVariable(variable));
    }

    public QFollow(Path<? extends Follow> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFollow(PathMetadata metadata) {
        super(Follow.class, metadata);
    }

}

