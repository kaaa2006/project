package org.team.mealkitshop.dto.admin;

import lombok.*;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminOrderDTO {

    private Long orderId;
    private String orderNo;
    private String buyerEmail;
    private String buyerName;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private Pay payMethod;
    private int payableAmount;

    private List<String> items; // 상품명 리스트
}