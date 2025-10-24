package org.team.mealkitshop.domain.order;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId; // PK

    // 주문 (다대일)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 100)
    private String itemName; // 주문 당시 상품명 스냅샷

    // 상품 (다대일)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // 수량
    @Column(nullable = false)
    private int quantity;

    /** 주문 시점의 단가 스냅샷 (Item.getSalePrice()를 고정 저장) */
    @Column(name = "purchase_price", nullable = false)
    private int purchasePrice;

    /** 라인 총액 = 스냅샷 단가 × 수량 */
    public int getLineTotal() {
        return purchasePrice * quantity;
    }

    /** 현재 구조에서는 상품 자체 할인 외 추가 할인은 서비스에서 처리 → 0 */
    public int getLineDiscount() {
        return 0;
    }

    /** 생성 편의 메서드: 주문 시점 단가 스냅샷을 반드시 세팅 */
    public static OrderItem of(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("item is null");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        return OrderItem.builder()
                .item(item)
                .itemName(item.getItemNm())       // ✅ 주문 당시 상품명 스냅샷 저장
                .quantity(quantity)
                .purchasePrice(item.getSalePrice()) // ✅ 주문 당시 판매가 스냅샷 저장
                .build();
    }

    /** 혹시 빌더/세터로 purchasePrice를 누락했을 때 안전장치 */
    @PrePersist
    private void prePersist() {
        if (purchasePrice <= 0 && item != null) {
            this.purchasePrice = item.getSalePrice();
        }
    }
}
