package org.team.mealkitshop.dto.checkout;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewResponse {
    // 👉 체크아웃(결제 전 미리보기) 화면 전체 응답 데이터
    //    프론트(화면)는 이 DTO 하나로 결제 페이지 전체를 구성할 수 있습니다.

    /**
     * 결제 페이지에 표시할 상품 목록
     * - 각 항목은 CheckoutPreviewLine 형태
     * - 예: [라면 3개, 우유 2개, 과자 1개]
     */
    private List<CheckoutPreviewLine> lines;

    /**
     * 총 상품금액 합계
     * - 모든 주문 상품의 (판매가 × 수량) 합
     * - 예: 5,000 × 2 + 10,000 × 1 = 20,000원
     */
    private int productsTotal;

    /**
     * 총 할인금액 합계
     * - 모든 상품의 (정상가 − 판매가) × 수량의 합
     * - 예: (6,000 - 5,000) × 2 = 2,000원
     */
    private int discountTotal;

    /**
     * 배송비
     * - 정책에 따라 무료(0원) 또는 일정 금액
     * - 예: 3만원 이상 구매 시 무료, 그 외 3,500원
     */
    private int shippingFee;

    /**
     * 최종 결제금액
     * - 공식: (총 상품금액) − (총 할인금액) + (배송비)
     * - 실제 결제 단계에서 고객이 지불해야 할 최종 금액
     */
    private int payableAmount;
}

//lines → 주문 상품 목록(한 줄씩)

//productsTotal → 총 상품금액

//discountTotal → 총 할인금액

//shippingFee → 배송비

//payableAmount → 최종 결제금액

//사용 흐름:

//Controller: CheckoutPreviewRequest를 받아 Service 호출

//Service: 선택된 CartItem들을 CheckoutPreviewLine 리스트로 변환

//금액 합계 계산 후 CheckoutPreviewResponse 생성

//프론트: 이 응답을 받아 결제 페이지 전체를 렌더링