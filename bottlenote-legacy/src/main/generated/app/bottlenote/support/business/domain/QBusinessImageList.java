package app.bottlenote.support.business.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBusinessImageList is a Querydsl query type for BusinessImageList
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QBusinessImageList extends BeanPath<BusinessImageList> {

    private static final long serialVersionUID = 1107181557L;

    public static final QBusinessImageList businessImageList = new QBusinessImageList("businessImageList");

    public final ListPath<BusinessImage, QBusinessImage> businessImages = this.<BusinessImage, QBusinessImage>createList("businessImages", BusinessImage.class, QBusinessImage.class, PathInits.DIRECT2);

    public QBusinessImageList(String variable) {
        super(BusinessImageList.class, forVariable(variable));
    }

    public QBusinessImageList(Path<? extends BusinessImageList> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBusinessImageList(PathMetadata metadata) {
        super(BusinessImageList.class, metadata);
    }

}

