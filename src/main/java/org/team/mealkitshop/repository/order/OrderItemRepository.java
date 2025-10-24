package org.team.mealkitshop.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.order.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 특정 주문의 모든 라인 아이템
    List<OrderItem> findByOrder_OrderId(Long orderId);

    // 특정 주문 + 상품이 이미 포함됐는지 확인 (중복 방지 등)
    boolean existsByOrder_OrderIdAndItem_Id(Long orderId, Long id);

    // 주문 ID 목록으로 한 번에 조회 (배치 로딩 최적화용)
    List<OrderItem> findByOrder_OrderIdIn(List<Long> orderIds);

    // 주문 삭제 전에 라인 먼저 지우는 경우 (orphanRemoval 안 쓰는 경우)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional // @Service 단에서 트랜잭션 있으면 생략 가능
    @Query("delete from OrderItem oi where oi.order.orderId = :orderId")
    void deleteByOrder_OrderId(@Param("orderId") Long orderId);

    // 라인 + 상품까지 fetch join (상세 조회용)
    @Query("""
                select oi from OrderItem oi
                join fetch oi.item
                where oi.order.orderId = :orderId
            """)
    List<OrderItem> findWithItemByOrderId(@Param("orderId") Long orderId);

    // 특정 상품이 어떤 결제방식의 주문에 포함됐는지 조회 (예: 통계용)
    @Query("""
    select oi from OrderItem oi
    join fetch oi.order o
    where oi.item.id = :itemId
      and o.payMethod = :payMethod
""")
    List<OrderItem> findByItemIdAndPayMethod(@Param("itemId") Long itemId,
                                             @Param("payMethod") Pay payMethod);
    /* 상품 주문이력 여부 판단 (상품쪽에서 추가) 상품 삭제 시 상품주문 이력 배제하고 삭제하기 위함 */
    boolean existsByItem_Id(Long itemId);

    /* 아이템에서 추가 상품 영구삭제 시 주문 이력도 삭제*/
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from OrderItem oi where oi.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
