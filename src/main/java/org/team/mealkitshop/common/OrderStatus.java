package org.team.mealkitshop.common;

public enum OrderStatus {
    CREATED("주문 생성"),
    PREPARING("상품 준비중"),
    SHIPPED("배송중"),
    DELIVERED("배송완료"),
    COMPLETED("주문 완료"),
    CANCELED("취소됨"),
    REFUND_REQUESTED("환불 요청됨"), // 🆕 추가
    REFUNDED("환불 완료");           // 기존 REFUNDED

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}