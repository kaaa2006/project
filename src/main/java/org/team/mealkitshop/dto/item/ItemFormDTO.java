package org.team.mealkitshop.dto.item;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ItemFormDTO {
    // 관리자용 상품 등록 및 수정 DTO

    private Long id;

    @NotBlank(message = "상품명은 필수 입력 값입니다.")
    private String itemNm;

    /** 정가 */
    @NotNull(message = "정가는 필수 입력 값입니다.")
    @Min(value = 0, message = "정가는 0 이상이어야 합니다.")
    private Integer originalPrice;

    /** 할인율(0~95). 0이면 할인 없음 */
    @NotNull
    @Min(0) @Max(95)
    private Integer discountRate = 0;

    // NotBlank 제거 (이미지)
    private String itemDetail;

    @NotNull(message = "재고는 필수 입력 값입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stockNumber;

    private ItemSellStatus itemSellStatus;

    private Category category;

    @NotNull(message = "음식 종류는 필수 선택 값입니다.")
    private FoodItem foodItem;

    private List<ItemImgDTO> itemImgDTOList = new ArrayList<>();
    private List<Long> itemImgIds = new ArrayList<>();

    private static final ModelMapper modelMapper = new ModelMapper();

    public Item createItem() {
        setFoodItem(this.foodItem);        // DTO 내부 동기화
        Item entity = modelMapper.map(this, Item.class);
        entity.setFoodItem(this.foodItem); // ← 엔티티 측 최종 동기화(권장)
        return entity;
    }

    /** foodItem 설정 시 category 자동 동기화 */
    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
    }

    public static ItemFormDTO of(Item item) {
        ItemFormDTO dto = modelMapper.map(item, ItemFormDTO.class);
        // 역매핑 후에도 동기화(보수적)
        dto.setFoodItem(dto.getFoodItem());
        return dto;
    }

    /** 상세 DTO → 폼 DTO (관리자 수정 초기값 세팅용) */
    public static ItemFormDTO of(ItemDTO item) {
        ItemFormDTO dto = new ItemFormDTO();
        dto.setId(item.getId());
        dto.setItemNm(item.getItemNm());
        dto.setOriginalPrice(item.getOriginalPrice());
        dto.setDiscountRate(item.getDiscountRate());
        dto.setStockNumber(item.getStockNumber());
        dto.setItemDetail(item.getItemDetail());
        dto.setItemSellStatus(item.getItemSellStatus());
        dto.setFoodItem(item.getFoodItem());
        return dto;
    }
}
