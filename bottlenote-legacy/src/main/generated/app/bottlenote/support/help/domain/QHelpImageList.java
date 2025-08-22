package app.bottlenote.support.help.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHelpImageList is a Querydsl query type for HelpImageList
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QHelpImageList extends BeanPath<HelpImageList> {

    private static final long serialVersionUID = -603195725L;

    public static final QHelpImageList helpImageList = new QHelpImageList("helpImageList");

    public final ListPath<HelpImage, QHelpImage> helpImages = this.<HelpImage, QHelpImage>createList("helpImages", HelpImage.class, QHelpImage.class, PathInits.DIRECT2);

    public QHelpImageList(String variable) {
        super(HelpImageList.class, forVariable(variable));
    }

    public QHelpImageList(Path<? extends HelpImageList> path) {
        super(path.getType(), path.getMetadata());
    }

    public QHelpImageList(PathMetadata metadata) {
        super(HelpImageList.class, metadata);
    }

}

