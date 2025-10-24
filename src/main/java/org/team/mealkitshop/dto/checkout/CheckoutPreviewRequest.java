package org.team.mealkitshop.dto.checkout;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewRequest {
    // 👉 장바구니에서 "선택 상품 주문" 버튼을 눌렀을 때 서버로 보내는 요청 DTO
    //    서버는 이 요청을 받아 결제 페이지에 필요한 상품 목록을 구성합니다.

    /**
     * 주문할 회원의 식별자
     * - Member 엔티티의 PK(mno) 값
     * - 어떤 사용자가 주문하는지 식별
     */
    private Long memberMno;

    /**
     * 주문할 장바구니 항목 ID 목록
     * - 선택 주문 시: 체크된 항목(cartItemId)들만 담아 전송
     * - 비어 있거나 null이면: 서버가 CartItem.checked = true 인 모든 항목을 자동 사용
     * - 예: [5, 8, 9] → 장바구니 5번, 8번, 9번 항목만 결제 목록에 포함
     */
    private List<Long> cartItemIds;
}

//기능: "이 상품들을 결제 페이지에서 보여줘"라는 요청.

//필드:
//memberMno → 누가 주문하는지
//cartItemIds → 어떤 장바구니 항목들을 주문할지

//흐름:
//프론트(장바구니 화면)에서 체크된 항목들의 ID 목록을 담아 서버로 전송.
//Controller → Service에서 이 목록 기반으로 CartItem을 조회하고,
//CheckoutPreviewLine 리스트로 변환하여 응답.

