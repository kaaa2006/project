package org.team.mealkitshop.domain.cart;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.item.Item;

@Entity
@Table(
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "item_id"}),
        indexes = {
                @Index(name = "idx_cart_item_cart", columnList = "cart_id"),
                @Index(name = "idx_cart_item_item", columnList = "item_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart", "item"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long cartItemId;

    // ✅ 어떤 장바구니(cart)에 담겼는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // ✅ 어떤 상품(item)을 담았는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // ✅ 담긴 수량 (기본 1, 1 이상만 허용)
    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    // ✅ 구매 체크 여부 (기본 true)
    @Column(nullable = false)
    @Builder.Default
    private boolean checked = true;

    // ✅ 총 정가 (가격 x 수량)
    public int getLineTotal() {
        return Math.max(0, item.getSalePrice()) * Math.max(1, quantity);
    }

    // ✅ 총 할인액 (할인 정보 없으므로 기본 0)
    public int getLineDiscountTotal() {
        if (item == null) return 0;
        int orig = Math.max(0, item.getOriginalPrice());
        int sale = Math.max(0, item.getSalePrice());
        int qty  = Math.max(1, quantity);
        int per  = Math.max(0, orig - sale);
        return per * qty;
    }

    // ✅ 실제 결제 금액 (할인 반영 필요 시 수정)
    public int getLinePayable() {
        if (item == null) return 0;
        int unit = Math.max(0, item.getSalePrice());
        int qty  = Math.max(1, quantity);
        return unit * qty; // 현재는 정가와 동일
    }


    // ✅ 수량을 지정 값으로 변경 (1 미만 안됨)
    public void changeQuantity(int q) {
        if (q < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = q;
    }

    // ✅ 수량을 증가시키거나 감소 (1 미만 불가)
    public void increaseQuantity(int delta) {
        int next = this.quantity + delta;
        if (next < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = next;
    }

    // ✅ 체크 상태 반전 (true <-> false)
    public void toggleChecked() {
        this.checked = !this.checked;
    }

    // ✅ cart 연관관계 수동 설정
    public void setCart(Cart cart) {
        this.cart = cart;
    }

    // ✅ item 연관관계 수동 설정
    public void setItem(Item item) {
        this.item = item;
    }

    // ✅ checked 필드의 boolean getter
    public boolean isChecked() {
        return checked;
    }
}
