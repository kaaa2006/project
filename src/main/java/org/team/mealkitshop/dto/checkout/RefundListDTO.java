package org.team.mealkitshop.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundListDTO {
    private Long orderId;
    private String orderNo;
    private LocalDateTime orderedAt;
    private String status;
    private String statusDescription;
    private int payableAmount;
    private List<String> itemNames;

    public static RefundListDTO from(Order order) {
        return RefundListDTO.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .orderedAt(order.getOrderDate())
                .status(order.getStatus().name())
                .statusDescription(order.getStatus().getDescription())
                .payableAmount(order.getPayableAmount())
                .itemNames(order.getOrderItems().stream()
                        .map(OrderItem::getItemName)
                        .collect(Collectors.toList()))
                .build();
    }
}
