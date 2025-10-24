package org.team.mealkitshop.dto.checkout;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequestDto {

    private Long orderId;        // 주문 ID
    private String reasonCode;   // 문자열로 받음 (폼에서 넘어오는 값)
    private String reasonDetail; // 기타 상세 사유

    public RefundRequestDto() {}

    public RefundRequestDto(Long orderId, String reasonCode, String reasonDetail) {
        this.orderId = orderId;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
    }
}
