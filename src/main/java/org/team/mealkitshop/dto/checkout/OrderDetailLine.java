package org.team.mealkitshop.dto.checkout;

import lombok.*;
import org.team.mealkitshop.domain.order.OrderItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailLine {
    private Long orderItemId;   // 주문 항목 PK
    private Long itemId;        // 상품 PK
    private String itemName;    // 상품명 (스냅샷)
    private int listPrice;      // 정상가
    private int salePrice;      // 판매가(주문 시점)
    private int quantity;       // 수량
    private int lineTotal;      // 합계
    private int lineDiscount;   // 할인액
    private String repImgUrl;   // 상품 이미지

    /**
     * OrderItem → DTO 변환
     */
    public static OrderDetailLine from(OrderItem oi) {
        return OrderDetailLine.builder()
                .orderItemId(oi.getOrderItemId())
                .itemId(oi.getItem().getId())
                .itemName(oi.getItemName() != null ? oi.getItemName() : oi.getItem().getItemNm())
                .listPrice(oi.getItem().getOriginalPrice())
                .salePrice(oi.getPurchasePrice())
                .quantity(oi.getQuantity())
                .lineTotal(oi.getLineTotal())
                .lineDiscount((oi.getItem().getOriginalPrice() - oi.getPurchasePrice()) * oi.getQuantity())
                .repImgUrl(
                        oi.getItem().getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.getRepimgYn()))
                                .map(img -> img.getImgUrl())
                                .findFirst()
                                .orElse("/img/No_Image.jpg")
                )
                .build();
    }

}
