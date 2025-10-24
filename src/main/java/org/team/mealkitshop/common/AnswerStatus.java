package org.team.mealkitshop.common;

public enum AnswerStatus { // 1:1 문의 답변 상태

    PENDING("답변 대기"),
    ANSWERED("답변 완료");

    private final String displayName;

    AnswerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}