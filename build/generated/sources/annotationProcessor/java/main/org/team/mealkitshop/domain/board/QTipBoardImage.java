package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTipBoardImage is a Querydsl query type for TipBoardImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTipBoardImage extends EntityPathBase<TipBoardImage> {

    private static final long serialVersionUID = -1948431414L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTipBoardImage tipBoardImage = new QTipBoardImage("tipBoardImage");

    public final StringPath fileName = createString("fileName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QTipBoard tipBoard;

    public QTipBoardImage(String variable) {
        this(TipBoardImage.class, forVariable(variable), INITS);
    }

    public QTipBoardImage(Path<? extends TipBoardImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTipBoardImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTipBoardImage(PathMetadata metadata, PathInits inits) {
        this(TipBoardImage.class, metadata, inits);
    }

    public QTipBoardImage(Class<? extends TipBoardImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.tipBoard = inits.isInitialized("tipBoard") ? new QTipBoard(forProperty("tipBoard")) : null;
    }

}

