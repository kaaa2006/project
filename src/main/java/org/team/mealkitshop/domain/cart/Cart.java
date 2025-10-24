package org.team.mealkitshop.domain.cart;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction; // ✅ 합계 공통화에 필요

/**
 * <h1>Cart (장바구니) 엔티티</h1>
 *
 * (주석은 기존 상세 설명 유지)
 */
@Entity
@Table(
        name = "cart",
        uniqueConstraints = @UniqueConstraint(columnNames = "member_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"member", "items"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart extends BaseTimeEntity {

    // --------------------------------------------------------------------
    // 🚚 배송 관련 상수(모두 KRW, 원 단위)
    // --------------------------------------------------------------------
    private static final int FREE_SHIPPING_THRESHOLD = 50_000;
    private static final int BASE_SHIPPING_FEE       = 3_000;
    private static final int JEJU_EXTRA_FEE          = 5_000;

    // --------------------------------------------------------------------
    // 🧾 기본 필드 (식별자/연관관계/컬렉션)
    // --------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long cartId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("cartItemId ASC")
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ====================================================================
    // 🏗 생성/연관관계 관리 메서드
    // ====================================================================
    public static Cart createFor(Member member) {
        Cart cart = new Cart();
        cart.member = member;
        cart.items = new ArrayList<>();
        return cart;
    }

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    public void clearItems() {
        for (CartItem item : new ArrayList<>(items)) {
            removeItem(item);
        }
    }

    // ====================================================================
    // 💰 합계 계산 (정가/할인/세일가)  — 중복 제거: sumBy(...) 공통 함수 사용
    // ====================================================================

    /** 전체 상품 정가 합계 */
    public int getProductsTotal() {
        return sumBy(CartItem::getLineTotal, false);
    }

    /** 전체 상품 할인 합계 */
    public int getDiscountTotal() {
        return sumBy(CartItem::getLineDiscountTotal, false);
    }

    /** 체크된 상품 정가 합계 */
    public int getCheckedProductsTotal() {
        return sumBy(CartItem::getLineTotal, true);
    }

    /** 체크된 상품 할인 합계 */
    public int getCheckedDiscountTotal() {
        return sumBy(CartItem::getLineDiscountTotal, true);
    }

    /** 전체 상품 세일가 합계 (실 결제 기준, 세일가 * 수량) */
    public int getSalePriceTotal() {
        return sumBy(CartItem::getLinePayable, false);
    }

    /** 체크된 상품 세일가 합계 (실 결제 기준, 세일가 * 수량) */
    public int getCheckedSalePriceTotal() {
        return sumBy(CartItem::getLinePayable, true);
    }

    // ====================================================================
    // 🚚 배송비/최종 결제금액 계산 — 중복 제거: shippingFeeFor(...) 공통 함수 사용
    // ====================================================================

    /** 예상 배송비(전체 담긴 상품 기준) */
    public int getEstimatedShippingFee(String zipcode) {
        return shippingFeeFor(getSalePriceTotal(), zipcode);
    }

    /** 예상 배송비(우편번호 미지정) */
    public int getEstimatedShippingFee() {
        return getEstimatedShippingFee(null);
    }

    /** 최종 결제금액 = (전체 세일가 합계) + (예상 배송비) */
    public int getPayableAmount(String zipcode) {
        int saleTotal = getSalePriceTotal();
        return saleTotal + shippingFeeFor(saleTotal, zipcode);
    }

    /** (체크된 항목 기준) 최종 결제금액 = 체크된 세일가 합계 + 배송비 */
    public int getCheckedPayableAmount(String zipcode) {
        int saleTotal = getCheckedSalePriceTotal();
        return saleTotal + shippingFeeFor(saleTotal, zipcode);
    }

    // ====================================================================
    // 🔽 내부 공통 헬퍼 (API 변경 없음)
    // ====================================================================

    /** 합계 공통 함수 (체크 여부 선택) */
    private int sumBy(ToIntFunction<CartItem> extractor, boolean onlyChecked) {
        return items.stream()
                .filter(ci -> !onlyChecked || ci.isChecked())
                .mapToInt(extractor)
                .sum();
    }

    /** 금액/우편번호에 따른 배송비 계산(단일 소스) */
    private int shippingFeeFor(int saleTotal, String zipcode) {
        int fee = (saleTotal >= FREE_SHIPPING_THRESHOLD) ? 0 : BASE_SHIPPING_FEE;
        if (zipcode != null && zipcode.startsWith("63")) {
            fee += JEJU_EXTRA_FEE;
        }
        return fee;
    }
}
