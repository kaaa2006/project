package org.team.mealkitshop.dto.checkout;

import lombok.*;
import org.team.mealkitshop.common.Pay;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    // 👉 체크아웃 화면에서 "결제하기" 버튼 클릭 시 서버로 보내는 데이터

    // ===== 회원 & 상품 =====
    private Long memberMno;               // 주문 회원 ID
    private List<Long> cartItemIds;       // 장바구니 항목 ID 목록

    // ===== 배송지 =====
    private Long addressId;               // 배송지 ID (Address PK)

    // ===== 수취인 정보 =====
    private String receiverName;          // 받는 사람 이름
    private String receiverPhone;         // 받는 사람 연락처
    private String zipCode;               // 우편번호
    private String address1;              // 기본 주소
    private String address2;              // 상세 주소
    private String memo;                  // 배송 메모(선택)

    // ===== 배송 옵션 =====
    public enum DeliveryMethod { PARCEL, DIRECT_PICKUP }
    private DeliveryMethod deliveryMethod;

    private LocalDate desiredDeliveryDate; // 희망 배송일 (선택)

    // ===== 혜택 적용 =====
    private String couponCode;             // 적용할 쿠폰 코드 (선택)
    private Integer usePoints;             // 사용할 포인트(선택, null 가능)

    // ===== 결제 =====
    private Pay payMethod;                 // ✅ 결제 방식 (무통장, 카드, 카카오, 네이버,토스 등)

    // ===== 약관 =====
    private Boolean termsAgreed;           // 구매 약관 동의 여부
}
