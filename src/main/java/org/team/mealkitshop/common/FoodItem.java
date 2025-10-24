package org.team.mealkitshop.common;

public enum FoodItem {
    SET_1WEEK(Category.SET, "세트 1주차"),
    SET_2WEEK(Category.SET, "세트 2주차"),
    SET_4WEEK(Category.SET, "세트 4주차"),
    SALAD(Category.REFRIGERATED, "샐러드"),
    POKE(Category.REFRIGERATED, "포케"),
    CHICKEN_BREAST(Category.FROZEN, "닭가슴살"),
    FRIED_RICE(Category.FROZEN, "볶음밥"),
    PROTEIN_DRINK(Category.ETC, "단백질 음료"),
    DRESSING(Category.ETC, "드레싱");

    private final Category category;
    private final String label;

    FoodItem(Category category, String label) {
        this.category = category;
        this.label = label;
    }
    public Category getCategory() { return category; }
    public String getLabel() { return label; }
}
