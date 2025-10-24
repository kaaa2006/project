package org.team.mealkitshop.common;


public enum BoardType {

    NOTICE("공지사항"),
    FAQ("자주 묻는 질문"),
    EVENT("이벤트 게시판");

    private final String label;

    BoardType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

