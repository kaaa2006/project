package org.team.mealkitshop.dto.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ItemDTO {
        // 상품 상세 보기에 대한 DTO

        private Long id;                // 상품 아이디
        private String itemNm;          // 상품명
        private Integer price;          // 실제가격(= 엔티티 getSalePrice() 결과)
        private int discountRate;       // 할인율(0~95)
        private Integer originalPrice;  // 정가
        private String repImgUrl;       // 상세 상단 단독 대표 표시
        private Integer stockNumber;    // 재고수
        private String itemDetail;      // 상세설명
        private ItemSellStatus itemSellStatus; // 판매 상태
        private Category category;             // 대분류
        private FoodItem foodItem;             // 중분류
        private Long itemLike;         // 좋아요 수
        private Long itemViewCnt;      // 조회 수
        private boolean liked;         // 로그인 사용자 기준 좋아요 토글 여부
        private Double avgRating;      // 평균 평점
        private Long reviewCount;      // 리뷰 개수

        private List<String> longImages = new ArrayList<>();

        // BaseTimeEntity
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime regTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;

        // BaseEntity
        private String createdBy;       // 생성자
        private String modifiedBy;      // 최종 수정자

        private List<ItemImgDTO> itemImages; // 상세 페이지용 이미지 목록
        private List<ReviewDTO> reviews;     // 상세 페이지용 리뷰 목록
}
