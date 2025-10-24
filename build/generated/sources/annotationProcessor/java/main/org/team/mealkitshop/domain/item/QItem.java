package org.team.mealkitshop.domain.item;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = -271601776L;

    public static final QItem item = new QItem("item");

    public final org.team.mealkitshop.common.QBaseEntity _super = new org.team.mealkitshop.common.QBaseEntity(this);

    public final ListPath<org.team.mealkitshop.domain.cart.CartItem, org.team.mealkitshop.domain.cart.QCartItem> cartitems = this.<org.team.mealkitshop.domain.cart.CartItem, org.team.mealkitshop.domain.cart.QCartItem>createList("cartitems", org.team.mealkitshop.domain.cart.CartItem.class, org.team.mealkitshop.domain.cart.QCartItem.class, PathInits.DIRECT2);

    public final EnumPath<org.team.mealkitshop.common.Category> category = createEnum("category", org.team.mealkitshop.common.Category.class);

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Integer> discountRate = createNumber("discountRate", Integer.class);

    public final EnumPath<org.team.mealkitshop.common.FoodItem> foodItem = createEnum("foodItem", org.team.mealkitshop.common.FoodItem.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ItemImage, QItemImage> images = this.<ItemImage, QItemImage>createList("images", ItemImage.class, QItemImage.class, PathInits.DIRECT2);

    public final StringPath itemDetail = createString("itemDetail");

    public final NumberPath<Long> itemLike = createNumber("itemLike", Long.class);

    public final StringPath itemNm = createString("itemNm");

    public final EnumPath<org.team.mealkitshop.common.ItemSellStatus> itemSellStatus = createEnum("itemSellStatus", org.team.mealkitshop.common.ItemSellStatus.class);

    public final NumberPath<Long> itemViewCnt = createNumber("itemViewCnt", Long.class);

    //inherited
    public final StringPath modifiedBy = _super.modifiedBy;

    public final NumberPath<Integer> originalPrice = createNumber("originalPrice", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final ListPath<Review, QReview> reviews = this.<Review, QReview>createList("reviews", Review.class, QReview.class, PathInits.DIRECT2);

    public final NumberPath<Integer> stockNumber = createNumber("stockNumber", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QItem(String variable) {
        super(Item.class, forVariable(variable));
    }

    public QItem(Path<? extends Item> path) {
        super(path.getType(), path.getMetadata());
    }

    public QItem(PathMetadata metadata) {
        super(Item.class, metadata);
    }

}

