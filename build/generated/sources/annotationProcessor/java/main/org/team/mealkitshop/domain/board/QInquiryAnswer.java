package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInquiryAnswer is a Querydsl query type for InquiryAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInquiryAnswer extends EntityPathBase<InquiryAnswer> {

    private static final long serialVersionUID = 1462914847L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInquiryAnswer inquiryAnswer = new QInquiryAnswer("inquiryAnswer");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final org.team.mealkitshop.domain.member.QMember admin;

    public final StringPath content = createString("content");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QInquiryBoard inquiryBoard;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QInquiryAnswer(String variable) {
        this(InquiryAnswer.class, forVariable(variable), INITS);
    }

    public QInquiryAnswer(Path<? extends InquiryAnswer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInquiryAnswer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInquiryAnswer(PathMetadata metadata, PathInits inits) {
        this(InquiryAnswer.class, metadata, inits);
    }

    public QInquiryAnswer(Class<? extends InquiryAnswer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new org.team.mealkitshop.domain.member.QMember(forProperty("admin")) : null;
        this.inquiryBoard = inits.isInitialized("inquiryBoard") ? new QInquiryBoard(forProperty("inquiryBoard"), inits.get("inquiryBoard")) : null;
    }

}

