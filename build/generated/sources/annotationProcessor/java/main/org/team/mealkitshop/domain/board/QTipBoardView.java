package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTipBoardView is a Querydsl query type for TipBoardView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTipBoardView extends EntityPathBase<TipBoardView> {

    private static final long serialVersionUID = 491720278L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTipBoardView tipBoardView = new QTipBoardView("tipBoardView");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final org.team.mealkitshop.domain.member.QMember member;

    public final QTipBoard tipBoard;

    public QTipBoardView(String variable) {
        this(TipBoardView.class, forVariable(variable), INITS);
    }

    public QTipBoardView(Path<? extends TipBoardView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTipBoardView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTipBoardView(PathMetadata metadata, PathInits inits) {
        this(TipBoardView.class, metadata, inits);
    }

    public QTipBoardView(Class<? extends TipBoardView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new org.team.mealkitshop.domain.member.QMember(forProperty("member")) : null;
        this.tipBoard = inits.isInitialized("tipBoard") ? new QTipBoard(forProperty("tipBoard")) : null;
    }

}

