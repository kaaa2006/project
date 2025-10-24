package org.team.mealkitshop.dto.checkout;

import lombok.*;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.domain.order.OrderItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentOrderDTO {
    private String orderNo;
    private String orderDate; // yyyy.MM.dd 포맷 문자열
    private String itemName;
    private int price;
    private int quantity;
    private String status;

    public static RecentOrderDTO from(OrderItem oi) {
        return RecentOrderDTO.builder()
                .orderNo(oi.getOrder().getOrderNo())
                .orderDate(oi.getOrder().getOrderDate()
                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .itemName(oi.getItem().getItemNm())
                .price(oi.getPurchasePrice() * oi.getQuantity())
                .quantity(oi.getQuantity())
                .status(oi.getOrder().getStatus().getDescription())
                .build();
    }
}

