package app.bottlenote.history.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserHistory is a Querydsl query type for UserHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserHistory extends EntityPathBase<UserHistory> {

    private static final long serialVersionUID = 1035511426L;

    public static final QUserHistory userHistory = new QUserHistory("userHistory");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final MapPath<String, String, StringPath> dynamicMessage = this.<String, String, StringPath>createMap("dynamicMessage", String.class, String.class, StringPath.class);

    public final EnumPath<app.bottlenote.history.constant.EventCategory> eventCategory = createEnum("eventCategory", app.bottlenote.history.constant.EventCategory.class);

    public final StringPath eventMonth = createString("eventMonth");

    public final EnumPath<app.bottlenote.history.constant.EventType> eventType = createEnum("eventType", app.bottlenote.history.constant.EventType.class);

    public final StringPath eventYear = createString("eventYear");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public final StringPath redirectUrl = createString("redirectUrl");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserHistory(String variable) {
        super(UserHistory.class, forVariable(variable));
    }

    public QUserHistory(Path<? extends UserHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserHistory(PathMetadata metadata) {
        super(UserHistory.class, metadata);
    }

}

