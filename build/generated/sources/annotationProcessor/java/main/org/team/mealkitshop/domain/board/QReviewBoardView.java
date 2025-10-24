package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewBoardView is a Querydsl query type for ReviewBoardView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewBoardView extends EntityPathBase<ReviewBoardView> {

    private static final long serialVersionUID = 1730620013L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewBoardView reviewBoardView = new QReviewBoardView("reviewBoardView");

    public final QReviewBoard board;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final org.team.mealkitshop.domain.member.QMember member;

    public QReviewBoardView(String variable) {
        this(ReviewBoardView.class, forVariable(variable), INITS);
    }

    public QReviewBoardView(Path<? extends ReviewBoardView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewBoardView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewBoardView(PathMetadata metadata, PathInits inits) {
        this(ReviewBoardView.class, metadata, inits);
    }

    public QReviewBoardView(Class<? extends ReviewBoardView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.board = inits.isInitialized("board") ? new QReviewBoard(forProperty("board"), inits.get("board")) : null;
        this.member = inits.isInitialized("member") ? new org.team.mealkitshop.domain.member.QMember(forProperty("member")) : null;
    }

}

