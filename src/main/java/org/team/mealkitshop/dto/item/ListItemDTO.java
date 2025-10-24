package org.team.mealkitshop.dto.item;

import lombok.*;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListItemDTO {
    // 사용자 보여지는 상품 리스트용 DTO

    private Long id;           // 상품 고유 번호
    private String itemNm;     // 상품명
    private String itemDetail; // 상품 설명
    private String repImgUrl;  // 상품 대표 이미지

    /** 실제 판매가(= 엔티티 getSalePrice() 결과) */
    private Integer price;

    /** 표시용 정가/할인율 */
    private Integer originalPrice;
    private int discountRate;

    private Long itemLike;     // 찜 수
    private Double avgRating;  // 평균 평점
    private Long reviewCount;  // 리뷰 개수
    private Long itemViewCnt;  // 조회 수

    private boolean liked;     // 로그인 사용자 기준 좋아요 여부

    private ItemSellStatus itemSellStatus;
    private LocalDateTime regTime;


    public static ListItemDTO of(Item item) {
        ListItemDTO dto = new ListItemDTO();
        dto.setId(item.getId());
        dto.setItemNm(item.getItemNm());
        dto.setItemDetail(item.getItemDetail());
        dto.setItemSellStatus(item.getItemSellStatus());
        dto.setRegTime(item.getRegTime());

        // 핵심: 엔티티 계산값으로 매핑
        dto.setPrice(item.getSalePrice());
        dto.setOriginalPrice(item.getOriginalPrice());
        dto.setDiscountRate(item.getDiscountRate());

        // 안전한 기본값(널 방어)
        if (dto.itemLike == null) dto.itemLike = 0L;
        if (dto.avgRating == null) dto.avgRating = 0.0;
        if (dto.reviewCount == null) dto.reviewCount = 0L;
        if (dto.itemViewCnt == null) dto.itemViewCnt = 0L;

        // 나머지는 필요시 채우기(대표이미지/지표/리뷰 등)
        return dto;
    }
}
