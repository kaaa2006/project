package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInquiryBoard is a Querydsl query type for InquiryBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInquiryBoard extends EntityPathBase<InquiryBoard> {

    private static final long serialVersionUID = 1295052645L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInquiryBoard inquiryBoard = new QInquiryBoard("inquiryBoard");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final QInquiryAnswer answer;

    public final StringPath content = createString("content");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> regTime = createDateTime("regTime", java.time.LocalDateTime.class);

    public final EnumPath<org.team.mealkitshop.common.AnswerStatus> status = createEnum("status", org.team.mealkitshop.common.AnswerStatus.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updateTime = createDateTime("updateTime", java.time.LocalDateTime.class);

    public final org.team.mealkitshop.domain.member.QMember writer;

    public QInquiryBoard(String variable) {
        this(InquiryBoard.class, forVariable(variable), INITS);
    }

    public QInquiryBoard(Path<? extends InquiryBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInquiryBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInquiryBoard(PathMetadata metadata, PathInits inits) {
        this(InquiryBoard.class, metadata, inits);
    }

    public QInquiryBoard(Class<? extends InquiryBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.answer = inits.isInitialized("answer") ? new QInquiryAnswer(forProperty("answer"), inits.get("answer")) : null;
        this.writer = inits.isInitialized("writer") ? new org.team.mealkitshop.domain.member.QMember(forProperty("writer")) : null;
    }

}

