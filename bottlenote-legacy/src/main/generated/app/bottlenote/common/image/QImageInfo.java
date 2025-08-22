package app.bottlenote.common.image;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QImageInfo is a Querydsl query type for ImageInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QImageInfo extends BeanPath<ImageInfo> {

    private static final long serialVersionUID = 1319766300L;

    public static final QImageInfo imageInfo = new QImageInfo("imageInfo");

    public final StringPath imageKey = createString("imageKey");

    public final StringPath imageName = createString("imageName");

    public final StringPath imagePath = createString("imagePath");

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Long> order = createNumber("order", Long.class);

    public QImageInfo(String variable) {
        super(ImageInfo.class, forVariable(variable));
    }

    public QImageInfo(Path<? extends ImageInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QImageInfo(PathMetadata metadata) {
        super(ImageInfo.class, metadata);
    }

}

