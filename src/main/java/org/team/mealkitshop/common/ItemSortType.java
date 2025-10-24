package org.team.mealkitshop.common;

public enum ItemSortType {
 // 리뷰에서도 활용 가능

 // POPULAR_LIKE,  // 찜 많은 순 (미사용)
 POPULAR_VIEW,  // 조회수 많은 순
 PRICE_ASC,     // 가격 낮은 순
 PRICE_DESC,    // 가격 높은 순
 RATING_DESC,    // 평균 평점 높은순
 REVIEW_DESC,     // 리뷰 수 많은순
 SALES_DESC,    // 판매량 많은 순
 NEW            // 최신순
}