package org.team.mealkitshop.common;

public enum Category {
    SET("세트"),
    REFRIGERATED("냉장"),
    FROZEN("냉동"),
    ETC("기타");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}


