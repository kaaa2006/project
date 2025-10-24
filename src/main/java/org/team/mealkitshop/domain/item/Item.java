package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.dto.item.ItemFormDTO;
import org.team.mealkitshop.exception.OutOfStockException;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id; // 상품코드 (PK)

    @Column(length = 50, nullable = false)
    private String itemNm; // 상품명

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice; // 정가

    @Column(name = "discount_rate")
    @Builder.Default
    private Integer discountRate = 0; // 할인율 (0~95)

    /** DB에는 저장하지 않고 계산식으로만 제공 */
    @Transient
    public int getSalePrice() {
        int rate = Math.max(0, Math.min(95, discountRate));
        return (int) Math.floor(originalPrice * (100 - rate) / 100.0);
    }

    @Column(nullable = false)
    private int stockNumber; // 재고수

    @Lob
    @Column(name = "item_detail", columnDefinition = "MEDIUMTEXT")
    private String itemDetail; // 상세 설명(이미지)

    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus; // 판매상태

    /** 장바구니 항목들 **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartitems = new ArrayList<>();

    /** 분류 (대분류/중분류) **/
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FoodItem foodItem;

    /** 지표 **/
    @Builder.Default
    @Column(nullable = false)
    private long itemLike = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long itemViewCnt = 0L;

    /** 리뷰 컬렉션 **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 50)
    private List<Review> reviews = new ArrayList<>();

    /** 이미지 컬렉션 **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    /* ===================== 편의 메서드 ===================== */

    // Review <-> Item
    public void addReview(Review review) {
        this.reviews.add(review);
        review.setItem(this);
    }

    public void removeReview(Review review) {
        this.reviews.remove(review);
        review.setItem(null);
    }

    // ItemImage <-> Item
    public void addImage(ItemImage image) {
        this.images.add(image);
        image.setItem(this);
    }

    public void removeImage(ItemImage image) {
        this.images.remove(image);
        image.setItem(null);
    }

    /* ===================== 대표 이미지 단일 진입점 ===================== */
    public void setRepresentative(ItemImage rep) {
        if (rep == null || rep.getItem() != this) {
            throw new IllegalArgumentException("대표이미지는 해당 Item에 속해야 합니다.");
        }
        for (ItemImage img : images) {
            img.clearRep();
        }
        rep.markRep();
    }

    /* ===================== 분류 동기화 ===================== */
    @Deprecated
    protected void setCategory(Category category) {
        this.category = category;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
    }

    public void syncSellStatusByStockIfNotStopped() {
        if (this.itemSellStatus == ItemSellStatus.STOP) return; // 관리자가 STOP 강제했으면 존중
        this.itemSellStatus = (this.stockNumber > 0)
                ? ItemSellStatus.SELL
                : ItemSellStatus.SOLD_OUT;
    }

    @PrePersist
    @PreUpdate
    private void onPersistOrUpdate() {
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
        if (this.itemSellStatus == null) {
            this.itemSellStatus = (this.stockNumber > 0)
                    ? ItemSellStatus.SELL
                    : ItemSellStatus.SOLD_OUT;
        }

        boolean found = false;
        for (ItemImage img : images) {
            if (img.isRep()) {
                if (!found) found = true;
                else img.clearRep();
            }
        }
        if (!found && !images.isEmpty()) images.get(0).markRep();
    }

    /* ===================== 기타 유틸 ===================== */

    public void updateItem(ItemFormDTO dto) {
        this.itemNm = dto.getItemNm();
        this.originalPrice = Math.max(0, defaultIfNull(dto.getOriginalPrice(), 0));
        int rate = defaultIfNull(dto.getDiscountRate(), 0);
        this.discountRate = Math.max(0, Math.min(95, rate)); // ← 가드
        this.stockNumber = Math.max(0, defaultIfNull(dto.getStockNumber(), 0));
        this.itemDetail = dto.getItemDetail();
        this.itemSellStatus = dto.getItemSellStatus();
        setFoodItem(dto.getFoodItem());
        syncSellStatusByStockIfNotStopped();
    }


    private static Integer defaultIfNull(Integer v, int def) { return v != null ? v : def; }

    public void increaseLike()    { this.itemLike++; }
    public void decreaseLike()    { if (this.itemLike > 0) this.itemLike--; }
    public void increaseViewCnt() { this.itemViewCnt++; }


    public void increaseStock(int qty) {
        if (qty < 1) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        this.stockNumber += qty;

        // ✅ 재고 보충되면 SELL 상태로 복구
        if (this.stockNumber > 0 && this.itemSellStatus == ItemSellStatus.SOLD_OUT) {
            this.itemSellStatus = ItemSellStatus.SELL;
        }
    }


    // ✅ 재고가 0이면 SOLD_OUT 전환
    public void decreaseStock(int qty) {
        if (qty < 1) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        if (this.stockNumber < qty) {
            throw new OutOfStockException("재고 부족: 요청수량=" + qty + ", 재고=" + this.stockNumber);
        }
        this.stockNumber -= qty;

        // ✅ 재고가 0이면 SOLD_OUT 전환
        if (this.stockNumber == 0) {
            this.itemSellStatus = ItemSellStatus.SOLD_OUT;
        }
    }

}
