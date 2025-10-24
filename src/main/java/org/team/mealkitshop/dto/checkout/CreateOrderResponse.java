package org.team.mealkitshop.dto.checkout;


import lombok.*;
import org.team.mealkitshop.common.OrderStatus;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResponse {
    // 👉 주문이 생성된 후 서버가 클라이언트(프론트)로 반환하는 응답 데이터
    //    결제 직후 주문 완료 화면에서 필요한 정보들을 포함합니다.

    /**
     * 주문 고유 ID
     * - DB에서 주문을 구분하는 기본 키(PK)
     * - 예: 101L → 101번 주문
     *
    private Long orderId;

    /**
     * 주문 번호
     * - 고객이 확인하기 쉽도록 만든 주문 식별자
     * - 형식 예: YYYYMMDD-XXXX (날짜 + 일련번호)
     */
    private String orderNo;

    /**
     * 주문 상태
     * - OrderStatus enum 사용
     * - 예: CREATED(생성됨), PAID(결제완료)
     */
    private OrderStatus status;

    // ===== 금액 요약 =====
    /**
     * 총 상품금액
     * - (판매가 × 수량)의 합계
     */
    private int productsTotal;

    /**
     * 총 할인금액
     * - (정상가 - 판매가) × 수량의 합계
     */
    private int discountTotal;

    /**
     * 배송비
     * - 무료 또는 정책에 따른 금액
     */
    private int shippingFee;

    /**
     * 최종 결제금액
     * - 총 상품금액 - 총 할인금액 + 배송비
     */
    private int payableAmount;
}

// 역할: 주문이 생성된 후 프론트에 보여줄 주문 완료 정보를 전달.
//주요 데이터:
//주문 식별: orderId, orderNo
//상태: status (CREATED, PAID 등)
//금액 요약: 상품금액, 할인금액, 배송비, 최종 결제금액
//사용 흐름:
//결제 성공 → 주문 DB 저장 → CreateOrderResponse 생성 → 프론트 전송
//프론트는 주문 완료 페이지에 이 값들을 표시