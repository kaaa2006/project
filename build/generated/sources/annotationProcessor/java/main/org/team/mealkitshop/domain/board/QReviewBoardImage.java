package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewBoardImage is a Querydsl query type for ReviewBoardImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewBoardImage extends EntityPathBase<ReviewBoardImage> {

    private static final long serialVersionUID = 2097722003L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewBoardImage reviewBoardImage = new QReviewBoardImage("reviewBoardImage");

    public final StringPath fileName = createString("fileName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QReviewBoard reviewBoard;

    public QReviewBoardImage(String variable) {
        this(ReviewBoardImage.class, forVariable(variable), INITS);
    }

    public QReviewBoardImage(Path<? extends ReviewBoardImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewBoardImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewBoardImage(PathMetadata metadata, PathInits inits) {
        this(ReviewBoardImage.class, metadata, inits);
    }

    public QReviewBoardImage(Class<? extends ReviewBoardImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reviewBoard = inits.isInitialized("reviewBoard") ? new QReviewBoard(forProperty("reviewBoard"), inits.get("reviewBoard")) : null;
    }

}

