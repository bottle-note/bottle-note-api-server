package app.bottlenote.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 141680475L;

    public static final QUser user = new QUser("user");

    public final app.bottlenote.common.domain.QBaseTimeEntity _super = new app.bottlenote.common.domain.QBaseTimeEntity(this);

    public final NumberPath<Integer> age = createNumber("age", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    public final StringPath email = createString("email");

    public final EnumPath<app.bottlenote.user.constant.GenderType> gender = createEnum("gender", app.bottlenote.user.constant.GenderType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    public final StringPath nickName = createString("nickName");

    public final StringPath password = createString("password");

    public final StringPath refreshToken = createString("refreshToken");

    public final EnumPath<app.bottlenote.user.constant.UserType> role = createEnum("role", app.bottlenote.user.constant.UserType.class);

    public final ListPath<app.bottlenote.user.constant.SocialType, EnumPath<app.bottlenote.user.constant.SocialType>> socialType = this.<app.bottlenote.user.constant.SocialType, EnumPath<app.bottlenote.user.constant.SocialType>>createList("socialType", app.bottlenote.user.constant.SocialType.class, EnumPath.class, PathInits.DIRECT2);

    public final StringPath socialUniqueId = createString("socialUniqueId");

    public final EnumPath<app.bottlenote.user.constant.UserStatus> status = createEnum("status", app.bottlenote.user.constant.UserStatus.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

