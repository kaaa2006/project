package org.team.mealkitshop.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.ReviewReply;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    /** 단건 조회: 리뷰ID로 답변 찾기 */
    Optional<ReviewReply> findByReview_Id(Long reviewId);

    /** 존재 여부: 리뷰ID에 이미 답변이 있는지 */
    boolean existsByReview_Id(Long reviewId);

    /** 벌크 조회: 여러 리뷰ID의 답변 모으기 */
    List<ReviewReply> findByReview_IdIn(Collection<Long> reviewIds);

    /** 단건 삭제: 리뷰ID 기준 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByReview_Id(Long reviewId);
}
