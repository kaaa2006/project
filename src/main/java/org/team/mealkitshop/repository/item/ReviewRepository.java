package org.team.mealkitshop.repository.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.Review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 아이템 별 리뷰 목록 (회원+아이템 페치) - 페이징 */
    @EntityGraph(attributePaths = {"member","item"}, type = EntityGraph.EntityGraphType.FETCH)
    Page<Review> findByItem_Id(Long itemId, Pageable pageable);

    /** 아이템 별 리뷰 + 이미지 페치 (N+1 방지) - 페이징 */
    @EntityGraph(attributePaths = {"member","item","images"}, type = EntityGraph.EntityGraphType.FETCH)
    @Query("select r from Review r where r.item.id = :itemId")
    Page<Review> findWithImagesByItemId(@Param("itemId") Long itemId, Pageable pageable);

    /** 아이템 상위 평점 10건 (회원까지 페치) */
    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Review> findTop10ByItem_IdOrderByRatingDescIdDesc(Long itemId);

    /** 회원이 작성한 리뷰 목록 (회원+아이템 페치) - 페이징 */
    @EntityGraph(attributePaths = {"member","item"}, type = EntityGraph.EntityGraphType.FETCH)
    Page<Review> findByMember_Mno(Long mno, Pageable pageable);

    /** 상세 화면 최적화: 단건 조회 시 member,item 함께 로드 */
    @EntityGraph(attributePaths = {"member","item"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<Review> findWithItemAndMemberById(Long id);

    /** 평균 평점 */
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.item.id = :itemId")
    Double getAverageRatingByItemId(@Param("itemId") Long itemId);

    /** 리뷰 개수 (Jpa 메서드) */
    long countByItem_Id(Long itemId);

    /** 회원이 해당 아이템에 리뷰 작성했는지 여부 */
    boolean existsByMember_MnoAndItem_Id(Long mno, Long itemId);

    /** 아이템의 리뷰 일괄 삭제 (영속 컨텍스트 자동 정리) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Review r where r.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);

    /** 여러 아이템 평균 평점 (프로젝션 권장) */
    @Query("""
           select r.item.id as itemId, avg(r.rating) as avgRating
             from Review r
            where r.item.id in :itemIds
            group by r.item.id
           """)
    List<ItemAvgRating> findAvgRatingByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /** 여러 아이템 리뷰 개수 (프로젝션 권장) */
    @Query("""
           select r.item.id as itemId, count(r.id) as reviewCount
             from Review r
            where r.item.id in :itemIds
            group by r.item.id
           """)
    List<ItemReviewCount> findReviewCountByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /** 대량 정리: 특정 아이템의 리뷰 ID만 경량 조회 */
    @Query("select r.id from Review r where r.item.id = :itemId")
    List<Long> findIdsByItemId(@Param("itemId") Long itemId);

    /* ===== Projections ===== */
    interface ItemAvgRating { Long getItemId(); Double getAvgRating(); }
    interface ItemReviewCount { Long getItemId(); Long getReviewCount(); }
}
