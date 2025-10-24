package org.team.mealkitshop.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.order.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /* ===================== 기본 조회 ===================== */

    // 📌 페이징 조회 (주소 + 주문상품 + 아이템까지 함께 로딩)
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    Page<Order> findByMember_MnoOrderByOrderDateDesc(Long mno, Pageable pageable);

    // 📌 전체 목록 조회 (주소 + 주문상품 + 아이템까지 함께 로딩)
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    List<Order> findByMember_MnoOrderByOrderDateDesc(Long mno);

    // 📌 단건 상세 조회 (본인 주문만, 상품까지 포함)
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    Optional<Order> findByOrderIdAndMember_Mno(Long orderId, Long mno);

    /* ===================== 상세 Fetch Join ===================== */

    @Query("""
        select distinct o from Order o
        join fetch o.member
        join fetch o.address
        left join fetch o.orderItems oi
        left join fetch oi.item
        where o.orderId = :orderId and o.member.mno = :memberMno
    """)
    Optional<Order> findByIdWithMember(@Param("orderId") Long orderId,
                                       @Param("memberMno") Long memberMno);

    /* ===================== 최근 주문 ===================== */

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    @Query("select distinct o from Order o where o.member.mno = :memberId order by o.orderDate desc")
    List<Order> findRecentOrders(@Param("memberId") Long memberId, Pageable pageable);

    /* ===================== 구매 이력 확인 ===================== */

    // 📌 상품 준비중 이상 주문 여부 (예: 상품 삭제 시 체크용)
    @Query("""
        select case when count(oi) > 0 then true else false end
          from Order o
          join o.orderItems oi
         where o.member.mno = :mno
           and oi.item.id   = :itemId
           and o.status in (
                org.team.mealkitshop.common.OrderStatus.PREPARING,
                org.team.mealkitshop.common.OrderStatus.SHIPPED,
                org.team.mealkitshop.common.OrderStatus.DELIVERED,
                org.team.mealkitshop.common.OrderStatus.COMPLETED
           )
    """)
    boolean hasPurchasedPreparingOrLater(@Param("mno") Long mno, @Param("itemId") Long itemId);

    // 📌 배송완료 이상 주문 여부 (리뷰 작성 가능 여부 체크용)
    @Query("""
        select case when count(oi) > 0 then true else false end
          from Order o
          join o.orderItems oi
         where o.member.mno = :mno
           and oi.item.id   = :itemId
           and o.status in (
                org.team.mealkitshop.common.OrderStatus.DELIVERED,
                org.team.mealkitshop.common.OrderStatus.COMPLETED
           )
    """)
    boolean hasPurchasedDeliveredOrLater(@Param("mno") Long mno, @Param("itemId") Long itemId);

    /* ===================== 관리자 ===================== */

    @EntityGraph(attributePaths = {"member"})
    @Query("select o from Order o order by o.orderDate desc")
    Page<Order> findAllWithMember(Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    @Query("""
        select o from Order o
        where (:status is null or o.status = :status)
        and   (:pay is null or o.payMethod = :pay)
        order by 
            case when :sort = 'name' then o.member.memberName end asc,
            case when :sort = 'date' or :sort is null then o.orderDate end desc
    """)
    Page<Order> searchOrders(@Param("status") OrderStatus status,
                             @Param("pay") Pay pay,
                             @Param("sort") String sort,
                             Pageable pageable);

    /** ✅ 회원별 특정 상태 주문 조회 */
    List<Order> findByMember_MnoAndStatusIn(Long memberId, List<OrderStatus> statuses);
}
