package org.team.mealkitshop.dto.item;

import lombok.Getter;
import lombok.Setter;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.common.ItemSortType;

@Getter @Setter
public class ItemSearchDTO {
    // 등록일 필터: "1d","1w","1m","6m"
    private String searchDateType;

    // 판매 상태: SELL | SOLD_OUT
    private ItemSellStatus itemSellStatus;

    // 검색 대상: itemNm | createdBy | itemDetail
    private String searchBy;

    // 검색어 (빈 문자열이면 미사용)
    private String searchQuery = "";

    // 기본 키워드(상품명 contains)
    private String keyword;

    // 카테고리
    private Category category;
    private FoodItem foodItem;

    // 정렬(enum) — 지정 시 우선, 미지정이면 Pageable.sort 사용
    // 값: NEW, PRICE_ASC, PRICE_DESC, RATING_DESC, REVIEW_DESC, POPULAR_VIEW
    private ItemSortType sortType;

    // 최저/최고가(실판매가 기준)
    private Integer minPrice;
    private Integer maxPrice;

    // 특가 상품 여부 (할인율 50% 이상)
    private Boolean specialDeal;

    // 신메뉴 여부
    private Boolean newItem;
}
