package org.team.mealkitshop.dto.checkout;

import lombok.*;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.order.Order;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {

    private Pay payMethod;

    private Long orderId;
    private String orderNo;
    private Long memberMno;

    private List<OrderDetailLine> lines;

    private int productsTotal;
    private int discountTotal;
    private int shippingFee;
    private int payableAmount;

    private OrderStatus status;   // enum 원본
    private String statusDesc;    // ✅ 한글 설명

    private LocalDateTime orderedAt;

    private String receiverName;
    private String receiverPhone;
    private String zipcode;
    private String addr1;
    private String addr2;
    private String memo;

    // DTO 변환 메서드
    public static OrderDetailResponse from(Order order) {
        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .memberMno(order.getMember().getMno())
                .status(order.getStatus())                  // 원본 enum
                .statusDesc(order.getStatus().getDescription()) // ✅ 한글 설명
                .orderedAt(order.getOrderDate())
                .productsTotal(order.getProductsTotal())
                .discountTotal(order.getDiscountTotal())
                .shippingFee(order.getShippingFee())
                .payableAmount(order.getPayableAmount())
                .receiverName(order.getAddress().getMember().getMemberName())
                .receiverPhone(order.getAddress().getMember().getPhone())
                .zipcode(order.getAddress().getZipCode())
                .addr1(order.getAddress().getAddr1())
                .addr2(order.getAddress().getAddr2())
                .memo(order.getMemo())
                .lines(order.getOrderItems().stream()
                        .map(OrderDetailLine::from)
                        .toList())
                .payMethod(order.getPayMethod())
                .build();
    }
}

