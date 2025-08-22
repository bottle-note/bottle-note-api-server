package app.external.notification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserDeviceToken is a Querydsl query type for UserDeviceToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserDeviceToken extends EntityPathBase<UserDeviceToken> {

    private static final long serialVersionUID = -263163493L;

    public static final QUserDeviceToken userDeviceToken = new QUserDeviceToken("userDeviceToken");

    public final DateTimePath<java.time.LocalDateTime> createAt = createDateTime("createAt", java.time.LocalDateTime.class);

    public final StringPath deviceToken = createString("deviceToken");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = createDateTime("lastModifyAt", java.time.LocalDateTime.class);

    public final EnumPath<app.external.notification.domain.constant.Platform> platform = createEnum("platform", app.external.notification.domain.constant.Platform.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserDeviceToken(String variable) {
        super(UserDeviceToken.class, forVariable(variable));
    }

    public QUserDeviceToken(Path<? extends UserDeviceToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserDeviceToken(PathMetadata metadata) {
        super(UserDeviceToken.class, metadata);
    }

}

