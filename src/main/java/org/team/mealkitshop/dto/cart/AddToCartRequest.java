package org.team.mealkitshop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * [장바구니 담기 요청 DTO]
 * - 클라이언트가 "상품을 장바구니에 담아주세요!" 라고 보낼 때 사용하는 객체
 * - POST /carts/items 요청에 사용됨
 *
 * 📌 예: {
 *   "memberId": 1,
 *   "itemId": 100,
 *   "quantity": 3
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    /** 회원 ID (필수) */
    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;

    /** 상품 ID (필수) */
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;

    /** 담을 수량 (1개 이상, 기본값 1) */
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Builder.Default
    private int quantity = 1;
}
