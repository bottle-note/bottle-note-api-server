package app.bottlenote.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRootAdmin is a Querydsl query type for RootAdmin
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRootAdmin extends EntityPathBase<RootAdmin> {

    private static final long serialVersionUID = 1979946493L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRootAdmin rootAdmin = new QRootAdmin("rootAdmin");

    public final QUser user;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRootAdmin(String variable) {
        this(RootAdmin.class, forVariable(variable), INITS);
    }

    public QRootAdmin(Path<? extends RootAdmin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRootAdmin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRootAdmin(PathMetadata metadata, PathInits inits) {
        this(RootAdmin.class, metadata, inits);
    }

    public QRootAdmin(Class<? extends RootAdmin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

