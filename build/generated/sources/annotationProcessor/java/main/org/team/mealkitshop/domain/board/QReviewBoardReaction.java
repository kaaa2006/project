package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewBoardReaction is a Querydsl query type for ReviewBoardReaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewBoardReaction extends EntityPathBase<ReviewBoardReaction> {

    private static final long serialVersionUID = 1454220785L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewBoardReaction reviewBoardReaction = new QReviewBoardReaction("reviewBoardReaction");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<org.team.mealkitshop.common.BoardReactionType> reaction = createEnum("reaction", org.team.mealkitshop.common.BoardReactionType.class);

    public final QReviewBoard reviewBoard;

    public final StringPath userId = createString("userId");

    public QReviewBoardReaction(String variable) {
        this(ReviewBoardReaction.class, forVariable(variable), INITS);
    }

    public QReviewBoardReaction(Path<? extends ReviewBoardReaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewBoardReaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewBoardReaction(PathMetadata metadata, PathInits inits) {
        this(ReviewBoardReaction.class, metadata, inits);
    }

    public QReviewBoardReaction(Class<? extends ReviewBoardReaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reviewBoard = inits.isInitialized("reviewBoard") ? new QReviewBoard(forProperty("reviewBoard"), inits.get("reviewBoard")) : null;
    }

}

