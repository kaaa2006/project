package org.team.mealkitshop.domain.order;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 * - 회원, 배송지, 주문상품, 금액 정보를 보관
 * - 배송지 스냅샷 없이 Address 엔티티를 직접 참조
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseTimeEntity {

    /** 주문 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    /** 주문번호 (YYYYMMDD-랜덤문자) */
    @Column(length = 40, unique = true, nullable = false)
    private String orderNo;

    /** 주문 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 배송지 (회원 배송지 엔티티 참조) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /** 주문 일시 */
    @Column(nullable = false)
    private LocalDateTime orderDate;

    /** 주문 상태 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)   // ✅ REFUND_REQUESTED(16자)까지 안전하게 저장 가능
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED; // ✅ 기본값 지정

    /** 상품 총액 */
    @Column(nullable = false)
    private int productsTotal;

    /** 총 할인 금액 */
    @Column(nullable = false)
    private int discountTotal;

    /** 배송비 */
    @Column(name = "shipping_fee", nullable = false)
    private int shippingFee;

    /** 최종 결제 금액 */
    @Column(nullable = false)
    private int payableAmount;

    /** 수취인 이름 */
    @Column(length = 20, nullable = false)
    private String receiverName;

    /** 수취인 연락처 */
    @Column(length = 20, nullable = false)
    private String receiverPhone;

    /** 주문 상세 상품 목록 */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /** 결제 방식 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Pay payMethod;

    /** 배송 요청사항 */
    @Column(length = 200)
    private String memo;

    /** 단일 상품 (예시: 간편 주문) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    // ===== 편의 메서드 =====
    public void addItem(OrderItem oi) {
        orderItems.add(oi);
        oi.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
        // TODO: 결제 취소/포인트 환불/재고 복구 등 추가 로직
    }
}
