package org.team.mealkitshop.domain.order;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = -651241872L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrder order = new QOrder("order1");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final org.team.mealkitshop.domain.member.QAddress address;

    public final NumberPath<Integer> discountTotal = createNumber("discountTotal", Integer.class);

    public final org.team.mealkitshop.domain.item.QItem item;

    public final org.team.mealkitshop.domain.member.QMember member;

    public final StringPath memo = createString("memo");

    public final DateTimePath<java.time.LocalDateTime> orderDate = createDateTime("orderDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public final ListPath<OrderItem, QOrderItem> orderItems = this.<OrderItem, QOrderItem>createList("orderItems", OrderItem.class, QOrderItem.class, PathInits.DIRECT2);

    public final StringPath orderNo = createString("orderNo");

    public final NumberPath<Integer> payableAmount = createNumber("payableAmount", Integer.class);

    public final EnumPath<org.team.mealkitshop.common.Pay> payMethod = createEnum("payMethod", org.team.mealkitshop.common.Pay.class);

    public final NumberPath<Integer> productsTotal = createNumber("productsTotal", Integer.class);

    public final StringPath receiverName = createString("receiverName");

    public final StringPath receiverPhone = createString("receiverPhone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final NumberPath<Integer> shippingFee = createNumber("shippingFee", Integer.class);

    public final EnumPath<org.team.mealkitshop.common.OrderStatus> status = createEnum("status", org.team.mealkitshop.common.OrderStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QOrder(String variable) {
        this(Order.class, forVariable(variable), INITS);
    }

    public QOrder(Path<? extends Order> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrder(PathMetadata metadata, PathInits inits) {
        this(Order.class, metadata, inits);
    }

    public QOrder(Class<? extends Order> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.address = inits.isInitialized("address") ? new org.team.mealkitshop.domain.member.QAddress(forProperty("address"), inits.get("address")) : null;
        this.item = inits.isInitialized("item") ? new org.team.mealkitshop.domain.item.QItem(forProperty("item")) : null;
        this.member = inits.isInitialized("member") ? new org.team.mealkitshop.domain.member.QMember(forProperty("member")) : null;
    }

}

