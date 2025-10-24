package org.team.mealkitshop.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PgCallbackRequest {
    private Long orderId;         // 우리 쪽 주문 ID
    private Integer amount;       // 결제 금액
    private String transactionId; // PG사에서 내려주는 결제번호
    private String status;        // SUCCESS / FAIL
    private String payMethod;     // CARD, KAKAO_PAY, NAVER_PAY 등
    private String signature;     // 위변조 검증용 해시
    private String timestamp;     // 결제 승인 시각
    private String paymentKey;  // 결제 키
}
