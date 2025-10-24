package org.team.mealkitshop.common;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoardBaseTimeEntity is a Querydsl query type for BoardBaseTimeEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBoardBaseTimeEntity extends EntityPathBase<BoardBaseTimeEntity> {

    private static final long serialVersionUID = -824699216L;

    public static final QBoardBaseTimeEntity boardBaseTimeEntity = new QBoardBaseTimeEntity("boardBaseTimeEntity");

    public final DateTimePath<java.time.LocalDateTime> regTime = createDateTime("regTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updateTime = createDateTime("updateTime", java.time.LocalDateTime.class);

    public QBoardBaseTimeEntity(String variable) {
        super(BoardBaseTimeEntity.class, forVariable(variable));
    }

    public QBoardBaseTimeEntity(Path<? extends BoardBaseTimeEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoardBaseTimeEntity(PathMetadata metadata) {
        super(BoardBaseTimeEntity.class, metadata);
    }

}

