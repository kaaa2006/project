package org.team.mealkitshop.dto.cart;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * [장바구니 항목 수정 요청 DTO]
 * - 장바구니에 담긴 상품의 수량/체크 상태를 변경할 때 사용
 * - 변경할 필드만 보내면 됨 (null 아닌 값만 적용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    /** 어떤 장바구니 항목을 수정할지 (필수) */
    @NotNull(message = "CartItem ID는 필수입니다.")
    private Long cartItemId;

    /**
     * 수량 변경 값 (선택)
     * - null이 아니면 해당 값으로 수량을 변경함
     * - 0이면 해당 항목을 삭제함
     */
    private Integer quantity;

    /**
     * 체크 상태 변경 여부 (선택)
     * - null이 아니면 이 값으로 checked 상태를 설정
     * - 예: true → 체크됨, false → 체크 해제
     */
    private Boolean checked;
}
