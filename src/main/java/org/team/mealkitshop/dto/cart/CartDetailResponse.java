package org.team.mealkitshop.dto.cart;

import lombok.*;

import java.util.List;

/**
 * [장바구니 상세 응답 DTO]
 * - 장바구니 전체 화면에서 필요한 정보를 담는 응답 객체
 * - 상품 목록 + 결제 금액 요약 정보를 함께 포함
 *
 * 언제 사용?
 * - 장바구니 페이지 진입 시 (GET /carts/{memberId})
 * - 장바구니 항목을 담거나 수정한 뒤 화면 갱신 시
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDetailResponse {

    /** 장바구니 ID */
    private Long cartId;

    /** 해당 장바구니의 소유 회원 ID */
    private Long memberId;

    /** 장바구니에 담긴 상품 목록 (각 항목은 CartItemDto) */
    private List<CartItemDto> items;

    /** 전체 항목 기준 합계 정보 (배송비 포함) */
    private AmountSummary summary;

    /** 체크된 항목 기준 합계 정보 (선택 결제용) */
    private AmountSummary checkedSummary;
}
