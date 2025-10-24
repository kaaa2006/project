package org.team.mealkitshop.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.RefundStatus;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderRefund;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.repository.order.OrderRefundRepository;
import org.team.mealkitshop.repository.order.OrderRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRefundService {

    private final OrderRefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    /**
     * 환불 요청 목록 전체 조회 (최신순)
     */
    public Page<OrderRefund> getRefundRequests(Pageable pageable) {
        return refundRepository.findAllByOrderByRegTimeDesc(pageable);
    }

    /**
     * 상태별 환불 요청 목록 조회 (최신순)
     */
    public Page<OrderRefund> getRefundRequestsByStatus(RefundStatus status, Pageable pageable) {
        return refundRepository.findByStatusOrderByRegTimeDesc(status, pageable);
    }

    /**
     * 환불 승인
     */
    @Transactional
    public void approveRefund(Long refundId, String adminNickname) {
        OrderRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청 없음"));

        Order order = refund.getOrder();
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 요청 상태에서만 승인 가능");
        }

        // 포인트 환불 처리
        if (order.getPayMethod().name().equals("POINT")) {
            Member member = order.getMember();
            member.setPoints(member.getPoints() + order.getPayableAmount());
        }

        // 재고 복구
        order.getOrderItems().forEach(oi -> oi.getItem().increaseStock(oi.getQuantity()));

        // 주문 상태 변경
        order.setStatus(OrderStatus.REFUNDED);

        // ✅ 환불 요청 상태 변경 (삭제 X)
        refund.setStatus(RefundStatus.APPROVED);
        refund.setProcessedBy(adminNickname);
        refund.setProcessedAt(LocalDateTime.now());
    }

    /**
     * 환불 거절
     */
    @Transactional
    public void rejectRefund(Long refundId, String adminNickname) {
        OrderRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청 없음"));

        Order order = refund.getOrder();
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 요청 상태에서만 거절 가능");
        }

        // 주문 상태 원상복구
        order.setStatus(OrderStatus.DELIVERED);

        // ✅ 환불 요청 상태 변경 (삭제 X)
        refund.setStatus(RefundStatus.REJECTED);
        refund.setProcessedBy(adminNickname);
        refund.setProcessedAt(LocalDateTime.now());
    }
}
