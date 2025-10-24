package org.team.mealkitshop.repository.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>,
        QuerydslPredicateExecutor<Item>, ItemRepositoryCustom {

    // 기본 CRUD(JpaRepository), 동적 쿼리(Querydsl), 커스텀 기능 제공

    // 이름 완전일치 검색
    List<Item> findByItemNm(String itemNm);

    // 가격
    // 실판매가 = FLOOR(original_price * (100 - discount_rate) / 100.0)
    @Query("""
       select i
         from Item i
        where floor(i.originalPrice * (100 - i.discountRate) / 100.0) < :price
       """)
    Page<Item> findBySalePriceLessThan(@Param("price") Integer price, Pageable pageable);

    // 이름/설명 부분검색 (대소문자 무시)
    Page<Item> findByItemNmContainingIgnoreCaseOrItemDetailContainingIgnoreCase(
            String name, String detail, Pageable pageable);

    // 카테고리, 판매상태 단독/복합 검색
    Page<Item> findByCategory(Category category, Pageable pageable);
    Page<Item> findByItemSellStatus(ItemSellStatus status, Pageable pageable);
    Page<Item> findByCategoryAndItemSellStatus(Category category, ItemSellStatus status, Pageable pageable);

    // 상세 조회: 리뷰만 즉시 로딩(EntityGraph)
    @EntityGraph(attributePaths = {"reviews"})
    @Query("SELECT DISTINCT i FROM Item i WHERE i.id = :id")
    Optional<Item> findWithReviewsById(@Param("id") Long id);

    // 상세 조회: 리뷰 + 작성자까지 즉시 로딩(EntityGraph)
    @EntityGraph(attributePaths = {"reviews", "reviews.member"})
    @Query("SELECT DISTINCT i FROM Item i WHERE i.id = :itemId")
    Optional<Item> findItemWithAllReviewsAndAuthors(@Param("itemId") Long itemId);

    /* 상세: 이미지까지만 즉시 로딩 (리뷰는 별도 페이징 API) */
    @EntityGraph(attributePaths = {"images"})
    @Query("select distinct i from Item i where i.id = :id")
    Optional<Item> findDetailBundleById(@Param("id") Long id);


    /** 조회수 +1 (엔티티 로딩 없이 벌크 업데이트) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.itemViewCnt = i.itemViewCnt + 1 where i.id = :id")
    int incrementView(@Param("id") Long id);

    @Query("select i from Item i left join fetch i.images where i.id = :id")
    Optional<Item> findByIdWithImages(@Param("id") Long id);

    /** 좋아요 +1 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.itemLike = i.itemLike + 1 where i.id = :id")
    int incrementLike(@Param("id") Long id);

    /** 재고 차감(음수 방지). 성공 시 1, 재고부족/미존재 시 0 반환 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Item i
              set i.stockNumber = i.stockNumber - :qty
            where i.id = :id
              and i.stockNumber >= :qty
           """)
    int decreaseStockSafely(@Param("id") Long id, @Param("qty") int qty);

    /* 좋아요 토글 관련 음수 방지 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Item i
              set i.itemLike = i.itemLike - 1
            where i.id = :id and i.itemLike > 0
           """)
    int decrementLikeSafely(@Param("id") Long id);

}

