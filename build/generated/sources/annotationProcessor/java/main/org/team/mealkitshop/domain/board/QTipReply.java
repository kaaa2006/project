package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTipReply is a Querydsl query type for TipReply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTipReply extends EntityPathBase<TipReply> {

    private static final long serialVersionUID = -386141163L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTipReply tipReply = new QTipReply("tipReply");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final org.team.mealkitshop.domain.member.QMember replyer;

    public final StringPath replyText = createString("replyText");

    public final NumberPath<Long> rno = createNumber("rno", Long.class);

    public final QTipBoard tipBoard;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QTipReply(String variable) {
        this(TipReply.class, forVariable(variable), INITS);
    }

    public QTipReply(Path<? extends TipReply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTipReply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTipReply(PathMetadata metadata, PathInits inits) {
        this(TipReply.class, metadata, inits);
    }

    public QTipReply(Class<? extends TipReply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.replyer = inits.isInitialized("replyer") ? new org.team.mealkitshop.domain.member.QMember(forProperty("replyer")) : null;
        this.tipBoard = inits.isInitialized("tipBoard") ? new QTipBoard(forProperty("tipBoard")) : null;
    }

}

