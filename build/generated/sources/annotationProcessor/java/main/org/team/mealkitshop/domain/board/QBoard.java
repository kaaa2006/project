package org.team.mealkitshop.domain.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoard is a Querydsl query type for Board
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoard extends EntityPathBase<Board> {

    private static final long serialVersionUID = 285709216L;

    public static final QBoard board = new QBoard("board");

    public final org.team.mealkitshop.common.QBoardBaseTimeEntity _super = new org.team.mealkitshop.common.QBoardBaseTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    public final NumberPath<Long> bno = createNumber("bno", Long.class);

    public final EnumPath<org.team.mealkitshop.common.BoardType> boardType = createEnum("boardType", org.team.mealkitshop.common.BoardType.class);

    public final StringPath content = createString("content");

    public final BooleanPath deleted = createBoolean("deleted");

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final SetPath<BoardImage, QBoardImage> imageSet = this.<BoardImage, QBoardImage>createSet("imageSet", BoardImage.class, QBoardImage.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public final StringPath writer = createString("writer");

    public QBoard(String variable) {
        super(Board.class, forVariable(variable));
    }

    public QBoard(Path<? extends Board> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoard(PathMetadata metadata) {
        super(Board.class, metadata);
    }

}

