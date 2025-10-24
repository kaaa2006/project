package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewBoard is a Querydsl query type for ReviewBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewBoard extends EntityPathBase<ReviewBoard> {

    private static final long serialVersionUID = 2107543464L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewBoard reviewBoard = new QReviewBoard("reviewBoard");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final NumberPath<Long> bno = createNumber("bno", Long.class);

    public final StringPath content = createString("content");

    public final NumberPath<Integer> helpfulCount = createNumber("helpfulCount", Integer.class);

    public final NumberPath<Integer> notHelpfulCount = createNumber("notHelpfulCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final ListPath<ReviewBoardReply, QReviewBoardReply> replies = this.<ReviewBoardReply, QReviewBoardReply>createList("replies", ReviewBoardReply.class, QReviewBoardReply.class, PathInits.DIRECT2);

    public final BooleanPath secretBoard = createBoolean("secretBoard");

    public final StringPath secretPassword = createString("secretPassword");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public final StringPath writer = createString("writer");

    public final org.team.mealkitshop.domain.member.QMember writerMember;

    public QReviewBoard(String variable) {
        this(ReviewBoard.class, forVariable(variable), INITS);
    }

    public QReviewBoard(Path<? extends ReviewBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewBoard(PathMetadata metadata, PathInits inits) {
        this(ReviewBoard.class, metadata, inits);
    }

    public QReviewBoard(Class<? extends ReviewBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.writerMember = inits.isInitialized("writerMember") ? new org.team.mealkitshop.domain.member.QMember(forProperty("writerMember")) : null;
    }

}

