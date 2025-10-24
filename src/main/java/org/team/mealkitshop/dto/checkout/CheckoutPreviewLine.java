package org.team.mealkitshop.dto.checkout;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewLine {
    // 👉 체크아웃(결제 전 미리보기) 화면에 표시될 "상품 한 줄" 정보
    //    결제 페이지에 들어갔을 때, 주문 상품 목록에서 보이는 한 행에 해당합니다.

    private Long cartItemId; // 장바구니 항목 ID (CartItem의 고유 식별자)
    // - 결제 시 어떤 장바구니 상품인지 식별하는 데 사용

    private Long itemId;     // 원본 상품 ID (Item의 고유 식별자)
    // - 상품 상세 페이지 이동 등에서 사용

    private String itemName; // 상품명 (담을 당시 스냅샷 또는 현재 상품명)

    private int listPrice;   // 정상가(정가)
    private int salePrice;   // 판매가(할인 적용된 가격)
    private int quantity;    // 주문 수량

    private int lineTotal;    // 한 줄 총액 = (판매가 × 수량)
    private int lineDiscount; // 한 줄 총 할인액 = ((정상가 - 판매가) × 수량)
}

// 기능: 결제 페이지의 "상품 목록 한 줄" 데이터.

//필드:
//상품 기본 정보: cartItemId, itemId, itemName
//가격 관련: listPrice, salePrice, quantity
//계산 값: lineTotal, lineDiscount

//흐름:
//Service에서 CartItem 정보를 기반으로 DTO를 생성.
//Controller가 이 리스트를 반환하면 프론트는 화면에 그대로 표시.