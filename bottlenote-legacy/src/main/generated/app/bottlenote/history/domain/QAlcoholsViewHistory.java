package app.bottlenote.history.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlcoholsViewHistory is a Querydsl query type for AlcoholsViewHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlcoholsViewHistory extends EntityPathBase<AlcoholsViewHistory> {

    private static final long serialVersionUID = 542591235L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAlcoholsViewHistory alcoholsViewHistory = new QAlcoholsViewHistory("alcoholsViewHistory");

    public final QAlcoholsViewHistory_AlcoholsViewHistoryId id;

    public final DateTimePath<java.time.LocalDateTime> viewAt = createDateTime("viewAt", java.time.LocalDateTime.class);

    public QAlcoholsViewHistory(String variable) {
        this(AlcoholsViewHistory.class, forVariable(variable), INITS);
    }

    public QAlcoholsViewHistory(Path<? extends AlcoholsViewHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAlcoholsViewHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAlcoholsViewHistory(PathMetadata metadata, PathInits inits) {
        this(AlcoholsViewHistory.class, metadata, inits);
    }

    public QAlcoholsViewHistory(Class<? extends AlcoholsViewHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QAlcoholsViewHistory_AlcoholsViewHistoryId(forProperty("id")) : null;
    }

}

