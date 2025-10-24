package org.team.mealkitshop.domain.order;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderRefund is a Querydsl query type for OrderRefund
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderRefund extends EntityPathBase<OrderRefund> {

    private static final long serialVersionUID = -358806008L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderRefund orderRefund = new QOrderRefund("orderRefund");

    public final org.team.mealkitshop.common.QBaseEntity _super = new org.team.mealkitshop.common.QBaseEntity(this);

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final StringPath modifiedBy = _super.modifiedBy;

    public final QOrder order;

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final StringPath processedBy = createString("processedBy");

    public final EnumPath<org.team.mealkitshop.common.RefundReason> reasonCode = createEnum("reasonCode", org.team.mealkitshop.common.RefundReason.class);

    public final StringPath reasonDetail = createString("reasonDetail");

    public final NumberPath<Long> refundId = createNumber("refundId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final EnumPath<org.team.mealkitshop.common.RefundStatus> status = createEnum("status", org.team.mealkitshop.common.RefundStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QOrderRefund(String variable) {
        this(OrderRefund.class, forVariable(variable), INITS);
    }

    public QOrderRefund(Path<? extends OrderRefund> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderRefund(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderRefund(PathMetadata metadata, PathInits inits) {
        this(OrderRefund.class, metadata, inits);
    }

    public QOrderRefund(Class<? extends OrderRefund> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

