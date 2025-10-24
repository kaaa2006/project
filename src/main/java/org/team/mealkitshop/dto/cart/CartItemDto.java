package org.team.mealkitshop.dto.cart;

import lombok.*;

/*
 * [장바구니 항목 응답 DTO]
 * - 장바구니 화면에 표시할 상품 1개(1줄) 정보를 담는 클래스
 * - 화면에 나오는 수량, 이름, 금액 등을 전달
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {

    /* 장바구니 항목 ID (식별자) */
    private Long cartItemId;

    /* 상품 ID */
    private Long itemId;

    /* ============ 상품 정보 (스냅샷) ============ */

    /* 상품 이름 (담을 당시 기준) */
    private String itemName;

    /* 정가 (할인 전 원래 가격) */
    private int originalPrice;

    /* 할인율 */
    private int discountRate;

    /* 판매가 (현재 판매 가격) */
    private int salePrice;

    /* 담은 수량 */
    private int quantity;

    /* 체크 여부 (결제 대상인지 여부) */
    private boolean checked;

    /* 🔹현재 재고(초과 입력 방지용) */
    private Integer stock;  // ← 추가 (필드 1개만) 09 05일

    /* ============ 금액 계산 결과 ============ */



    /** 정가 기준 총액 = 정가 × 수량 */
    private int lineTotal;

    /** 할인액 = (정가 - 판매가) × 수량 */
    private int lineDiscountTotal;

    /** 결제금액 = 판매가 × 수량 */
    private int linePayable;

    /** 상품 썸네일 (없을 수 있음 → null 허용) */
    private String thumbnailUrl;
}
