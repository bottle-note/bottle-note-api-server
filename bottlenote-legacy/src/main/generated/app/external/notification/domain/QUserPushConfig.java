package app.external.notification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserPushConfig is a Querydsl query type for UserPushConfig
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserPushConfig extends EntityPathBase<UserPushConfig> {

    private static final long serialVersionUID = -207472732L;

    public static final QUserPushConfig userPushConfig = new QUserPushConfig("userPushConfig");

    public final BooleanPath event = createBoolean("event");

    public final BooleanPath follower = createBoolean("follower");

    public final BooleanPath night = createBoolean("night");

    public final BooleanPath promotion = createBoolean("promotion");

    public final BooleanPath review = createBoolean("review");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserPushConfig(String variable) {
        super(UserPushConfig.class, forVariable(variable));
    }

    public QUserPushConfig(Path<? extends UserPushConfig> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserPushConfig(PathMetadata metadata) {
        super(UserPushConfig.class, metadata);
    }

}

