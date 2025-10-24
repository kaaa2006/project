package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewBoardReply is a Querydsl query type for ReviewBoardReply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewBoardReply extends EntityPathBase<ReviewBoardReply> {

    private static final long serialVersionUID = 2105809954L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewBoardReply reviewBoardReply = new QReviewBoardReply("reviewBoardReply");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final org.team.mealkitshop.domain.member.QMember replyer;

    public final StringPath replyText = createString("replyText");

    public final QReviewBoard reviewBoard;

    public final NumberPath<Long> rno = createNumber("rno", Long.class);

    public final BooleanPath secret = createBoolean("secret");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QReviewBoardReply(String variable) {
        this(ReviewBoardReply.class, forVariable(variable), INITS);
    }

    public QReviewBoardReply(Path<? extends ReviewBoardReply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewBoardReply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewBoardReply(PathMetadata metadata, PathInits inits) {
        this(ReviewBoardReply.class, metadata, inits);
    }

    public QReviewBoardReply(Class<? extends ReviewBoardReply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.replyer = inits.isInitialized("replyer") ? new org.team.mealkitshop.domain.member.QMember(forProperty("replyer")) : null;
        this.reviewBoard = inits.isInitialized("reviewBoard") ? new QReviewBoard(forProperty("reviewBoard"), inits.get("reviewBoard")) : null;
    }

}

