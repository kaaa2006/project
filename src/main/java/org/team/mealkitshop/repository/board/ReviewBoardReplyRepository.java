package org.team.mealkitshop.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.ReviewBoardReply;

import java.util.Collection;
import java.util.List;

// ReviewReplyRepository
// - 후기 게시글 댓글 관리
// - 댓글 목록 조회, 삭제 기능 제공
@Repository
public interface ReviewBoardReplyRepository extends JpaRepository<ReviewBoardReply, Long> {

    // 게시글에 달린 댓글 목록 조회 (페이징)
    Page<ReviewBoardReply> findByReviewBoard_Bno(Long reviewBoardId, Pageable pageable);

    // 게시글에 달린 댓글 전체 삭제
    void deleteAllByReviewBoard_Bno(Long reviewBoardId);

    // 게시글 댓글 수 조회 (테스트/검증용)
    long countByReviewBoard_Bno(Long reviewBoardId);

    // Review 게시글 댓글 최신순 조회
    List<ReviewBoardReply> findByReviewBoard_BnoOrderByRegTimeDesc(Long reviewBoardId);

    // ReviewBoardReply 전체 조회 (작성자 포함)
    List<ReviewBoardReply> findByReviewBoard_BnoOrderByRegTimeAsc(Long reviewBoardBno);
}