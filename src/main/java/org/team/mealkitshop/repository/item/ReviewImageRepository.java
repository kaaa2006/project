// src/main/java/org/team/mealkitshop/repository/item/ReviewImageRepository.java
package org.team.mealkitshop.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.ReviewImage;

import java.util.List;


@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /** 특정 리뷰의 이미지 전체 조회 */
    List<ReviewImage> findByReview_Id(Long reviewId);

    /** 특정 리뷰의 이미지 전체 조회 (등록순 정렬) */
    List<ReviewImage> findByReview_IdOrderByIdAsc(Long reviewId);

    /** 특정 아이템에 속한 모든 리뷰 이미지 조회 */
    List<ReviewImage> findByReview_Item_Id(Long itemId);

    /** 특정 리뷰의 모든 이미지 삭제 */
    void deleteByReview_Id(Long reviewId);

    /** 특정 리뷰의 이미지 일괄 삭제 (JPQL Bulk) */
    @Modifying
    @Query("delete from ReviewImage ri where ri.review.id=:reviewId")
    void deleteByReviewIdBulk(Long reviewId);

    /** 특정 아이템에 속한 모든 리뷰 이미지 일괄 삭제 (JPQL Bulk) */
    @Modifying
    @Query("delete from ReviewImage ri where ri.review.item.id=:itemId")
    void deleteByItemIdBulk(Long itemId);

    /** 특정 리뷰의 첫 번째 이미지(등록순) 조회 */
    ReviewImage findFirstByReview_IdOrderByIdAsc(Long reviewId);

    /** 특정 아이템에 달린 리뷰 이미지 총 개수 */
    long countByReview_Item_Id(Long itemId);

    /** 여러 리뷰에 속한 이미지들 일괄 조회 (등록순) */
    List<ReviewImage> findByReview_IdInOrderByIdAsc(List<Long> reviewIds);

    long countByReview_Id(Long reviewId); // 합산 장수 체크용
}
