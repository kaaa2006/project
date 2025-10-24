package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTipBoardReaction is a Querydsl query type for TipBoardReaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTipBoardReaction extends EntityPathBase<TipBoardReaction> {

    private static final long serialVersionUID = 754937178L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTipBoardReaction tipBoardReaction = new QTipBoardReaction("tipBoardReaction");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<org.team.mealkitshop.common.BoardReactionType> reaction = createEnum("reaction", org.team.mealkitshop.common.BoardReactionType.class);

    public final QTipBoard tipBoard;

    public final StringPath userId = createString("userId");

    public QTipBoardReaction(String variable) {
        this(TipBoardReaction.class, forVariable(variable), INITS);
    }

    public QTipBoardReaction(Path<? extends TipBoardReaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTipBoardReaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTipBoardReaction(PathMetadata metadata, PathInits inits) {
        this(TipBoardReaction.class, metadata, inits);
    }

    public QTipBoardReaction(Class<? extends TipBoardReaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.tipBoard = inits.isInitialized("tipBoard") ? new QTipBoard(forProperty("tipBoard")) : null;
    }

}

