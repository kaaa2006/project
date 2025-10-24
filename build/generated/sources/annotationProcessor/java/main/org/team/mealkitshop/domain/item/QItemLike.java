package org.team.mealkitshop.domain.item;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItemLike is a Querydsl query type for ItemLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItemLike extends EntityPathBase<ItemLike> {

    private static final long serialVersionUID = 443648839L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItemLike itemLike = new QItemLike("itemLike");

    public final org.team.mealkitshop.common.QBaseEntity _super = new org.team.mealkitshop.common.QBaseEntity(this);

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QItem item;

    public final org.team.mealkitshop.domain.member.QMember member;

    //inherited
    public final StringPath modifiedBy = _super.modifiedBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QItemLike(String variable) {
        this(ItemLike.class, forVariable(variable), INITS);
    }

    public QItemLike(Path<? extends ItemLike> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItemLike(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItemLike(PathMetadata metadata, PathInits inits) {
        this(ItemLike.class, metadata, inits);
    }

    public QItemLike(Class<? extends ItemLike> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new QItem(forProperty("item")) : null;
        this.member = inits.isInitialized("member") ? new org.team.mealkitshop.domain.member.QMember(forProperty("member")) : null;
    }

}

