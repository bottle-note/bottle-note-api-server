package app.bottlenote.picks.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPicks is a Querydsl query type for Picks
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPicks extends EntityPathBase<Picks> {

    private static final long serialVersionUID = -1558105459L;

    public static final QPicks picks = new QPicks("picks");

    public final app.bottlenote.common.domain.QBaseTimeEntity _super = new app.bottlenote.common.domain.QBaseTimeEntity(this);

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    public final EnumPath<app.bottlenote.picks.constant.PicksStatus> status = createEnum("status", app.bottlenote.picks.constant.PicksStatus.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPicks(String variable) {
        super(Picks.class, forVariable(variable));
    }

    public QPicks(Path<? extends Picks> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPicks(PathMetadata metadata) {
        super(Picks.class, metadata);
    }

}

