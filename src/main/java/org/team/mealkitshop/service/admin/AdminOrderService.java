package org.team.mealkitshop.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.repository.order.OrderRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;

    /** 관리자 전체 조회 */
    public Page<Order> getOrders(int page, int size, String sort, OrderStatus status, Pay pay) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status == null && pay == null && (sort == null || sort.isBlank())) {
            return orderRepository.findAllWithMember(pageable);
        }
        return orderRepository.searchOrders(status, pay, sort, pageable);
    }

    /** 주문 상태 변경 */
    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));
        order.setStatus(newStatus);
    }
}