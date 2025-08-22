package app.bottlenote.history.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAlcoholsViewHistory_AlcoholsViewHistoryId is a Querydsl query type for AlcoholsViewHistoryId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QAlcoholsViewHistory_AlcoholsViewHistoryId extends BeanPath<AlcoholsViewHistory.AlcoholsViewHistoryId> {

    private static final long serialVersionUID = 2024185274L;

    public static final QAlcoholsViewHistory_AlcoholsViewHistoryId alcoholsViewHistoryId = new QAlcoholsViewHistory_AlcoholsViewHistoryId("alcoholsViewHistoryId");

    public final NumberPath<Long> alcoholId = createNumber("alcoholId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAlcoholsViewHistory_AlcoholsViewHistoryId(String variable) {
        super(AlcoholsViewHistory.AlcoholsViewHistoryId.class, forVariable(variable));
    }

    public QAlcoholsViewHistory_AlcoholsViewHistoryId(Path<? extends AlcoholsViewHistory.AlcoholsViewHistoryId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAlcoholsViewHistory_AlcoholsViewHistoryId(PathMetadata metadata) {
        super(AlcoholsViewHistory.AlcoholsViewHistoryId.class, metadata);
    }

}

