package org.team.mealkitshop.common;

public enum RefundStatus {
    PENDING("대기"),
    APPROVED("승인됨"),
    REJECTED("거절됨");

    private final String description;

    RefundStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}