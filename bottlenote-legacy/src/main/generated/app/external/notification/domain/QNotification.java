package app.external.notification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotification is a Querydsl query type for Notification
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotification extends EntityPathBase<Notification> {

    private static final long serialVersionUID = -998010200L;

    public static final QNotification notification = new QNotification("notification");

    public final app.bottlenote.core.common.domain.QBaseEntity _super = new app.bottlenote.core.common.domain.QBaseEntity(this);

    public final EnumPath<app.external.notification.domain.constant.NotificationCategory> category = createEnum("category", app.external.notification.domain.constant.NotificationCategory.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isRead = createBoolean("isRead");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final EnumPath<app.external.notification.domain.constant.NotificationStatus> status = createEnum("status", app.external.notification.domain.constant.NotificationStatus.class);

    public final StringPath title = createString("title");

    public final EnumPath<app.external.notification.domain.constant.NotificationType> type = createEnum("type", app.external.notification.domain.constant.NotificationType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QNotification(String variable) {
        super(Notification.class, forVariable(variable));
    }

    public QNotification(Path<? extends Notification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotification(PathMetadata metadata) {
        super(Notification.class, metadata);
    }

}

