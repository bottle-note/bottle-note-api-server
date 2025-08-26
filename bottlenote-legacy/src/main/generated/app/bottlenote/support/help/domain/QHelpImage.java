package app.bottlenote.support.help.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHelpImage is a Querydsl query type for HelpImage
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHelpImage extends EntityPathBase<HelpImage> {

    private static final long serialVersionUID = -1272900491L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHelpImage helpImage = new QHelpImage("helpImage");

    public final app.bottlenote.common.domain.QBaseEntity _super = new app.bottlenote.common.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createAt = _super.createAt;

    //inherited
    public final StringPath createBy = _super.createBy;

    public final NumberPath<Long> helpId = createNumber("helpId", Long.class);

    public final app.bottlenote.common.image.QImageInfo helpimageInfo;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifyAt = _super.lastModifyAt;

    //inherited
    public final StringPath lastModifyBy = _super.lastModifyBy;

    public QHelpImage(String variable) {
        this(HelpImage.class, forVariable(variable), INITS);
    }

    public QHelpImage(Path<? extends HelpImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHelpImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHelpImage(PathMetadata metadata, PathInits inits) {
        this(HelpImage.class, metadata, inits);
    }

    public QHelpImage(Class<? extends HelpImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.helpimageInfo = inits.isInitialized("helpimageInfo") ? new app.bottlenote.common.image.QImageInfo(forProperty("helpimageInfo")) : null;
    }

}

