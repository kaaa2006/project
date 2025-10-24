package org.team.mealkitshop.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.RefundStatus;
import org.team.mealkitshop.domain.order.OrderRefund;

import java.util.List;

@Repository
public interface OrderRefundRepository extends JpaRepository<OrderRefund, Long> {

    // ✅ 특정 주문의 환불 내역 조회
    List<OrderRefund> findByOrder_OrderId(Long orderId);

    // ✅ 특정 회원의 모든 환불 내역 조회
    List<OrderRefund> findByOrder_Member_Mno(Long memberMno);

    // ✅ 상태별 환불 요청 조회 (최신순: regTime 기준)
    Page<OrderRefund> findByStatusOrderByRegTimeDesc(RefundStatus status, Pageable pageable);

    // ✅ 전체 환불 요청 조회 (최신순: regTime 기준)
    Page<OrderRefund> findAllByOrderByRegTimeDesc(Pageable pageable);
}
