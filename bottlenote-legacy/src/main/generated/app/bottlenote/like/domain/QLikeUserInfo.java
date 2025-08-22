package app.bottlenote.like.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLikeUserInfo is a Querydsl query type for LikeUserInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QLikeUserInfo extends BeanPath<LikeUserInfo> {

    private static final long serialVersionUID = 702201132L;

    public static final QLikeUserInfo likeUserInfo = new QLikeUserInfo("likeUserInfo");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final StringPath userNickName = createString("userNickName");

    public QLikeUserInfo(String variable) {
        super(LikeUserInfo.class, forVariable(variable));
    }

    public QLikeUserInfo(Path<? extends LikeUserInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLikeUserInfo(PathMetadata metadata) {
        super(LikeUserInfo.class, metadata);
    }

}

