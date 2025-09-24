package app.bottlenote.support.block.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserBlock is a Querydsl query type for UserBlock
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBlock extends EntityPathBase<UserBlock> {

    private static final long serialVersionUID = 299075937L;

    public static final QUserBlock userBlock = new QUserBlock("userBlock");

    public final app.bottlenote.core.common.domain.QBaseTimeEntity _super = new app.bottlenote.core.common.domain.QBaseTimeEntity(this);

    public final NumberPath<Long> blockedId = createNumber("blockedId", Long.class);

    public final NumberPath<Long> blockerId = createNumber("blockerId", Long.class);

    public final StringPath blockReason = createString("blockReason");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    public QUserBlock(String variable) {
        super(UserBlock.class, forVariable(variable));
    }

    public QUserBlock(Path<? extends UserBlock> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserBlock(PathMetadata metadata) {
        super(UserBlock.class, metadata);
    }

}

