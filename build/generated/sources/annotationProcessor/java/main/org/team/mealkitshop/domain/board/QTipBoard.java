package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTipBoard is a Querydsl query type for TipBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTipBoard extends EntityPathBase<TipBoard> {

    private static final long serialVersionUID = -400633839L;

    public static final QTipBoard tipBoard = new QTipBoard("tipBoard");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final NumberPath<Long> bno = createNumber("bno", Long.class);

    public final StringPath content = createString("content");

    public final NumberPath<Integer> dislikeCount = createNumber("dislikeCount", Integer.class);

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final ListPath<TipBoardReaction, QTipBoardReaction> reactions = this.<TipBoardReaction, QTipBoardReaction>createList("reactions", TipBoardReaction.class, QTipBoardReaction.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final ListPath<TipReply, QTipReply> replies = this.<TipReply, QTipReply>createList("replies", TipReply.class, QTipReply.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    public final NumberPath<Integer> topHelpful = createNumber("topHelpful", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public final StringPath writer = createString("writer");

    public final NumberPath<Long> writerId = createNumber("writerId", Long.class);

    public QTipBoard(String variable) {
        super(TipBoard.class, forVariable(variable));
    }

    public QTipBoard(Path<? extends TipBoard> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTipBoard(PathMetadata metadata) {
        super(TipBoard.class, metadata);
    }

}

