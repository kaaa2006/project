package org.team.mealkitshop.dto.cart;

import lombok.*;

/**
 * [장바구니 금액 요약 응답 DTO]
 * - 장바구니 하단에 표시되는 금액 관련 정보들을 담음
 * - 전체 항목 또는 체크된 항목 기준으로 계산 결과 응답
 *
 * 예시:
 * - 상품 합계: 50,000원
 * - 할인 금액: 5,000원
 * - 배송비: 3,000원
 * - 쿠폰 할인: 2,000원
 * - 최종 결제금액: 46,000원
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmountSummary {

    /** 상품 정가 합계 (모든 항목 또는 체크된 항목 기준) */
    private int productsTotal;

/*    *//** 전체 할인 합계 (항목 자체 할인) *//*
    private int discountTotal;*/

    /** 배송비 (정책/금액/지역에 따라 계산됨) */
    private int shippingFee;

    /** 최종 결제 금액 (할인, 배송비, 쿠폰 모두 반영 후) */
    private int payableAmount;

    /** 쿠폰 할인 금액 (없으면 0) */
    @Builder.Default
    private int couponDiscount = 0;

    /** 적용된 쿠폰 코드 또는 회원 등급 (예: "VIP", "GOLD") */
    private String appliedCouponCode;
}
