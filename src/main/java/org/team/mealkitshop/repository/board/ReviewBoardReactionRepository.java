package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReaction;

import java.util.Optional;

/**
 * ReviewBoardReaction JPA Repository
 * - ReviewBoard 게시글에 대한 사용자 반응(도움 됨 / 도움 안 됨) 관리
 * - 조회, 존재 여부 확인, 삭제 등
 */
@Repository
public interface ReviewBoardReactionRepository extends JpaRepository<ReviewBoardReaction, Long> {

    /** 특정 게시글 + 사용자 + 반응 타입 조회 */
    Optional<ReviewBoardReaction> findByReviewBoardAndUserIdAndReaction(ReviewBoard reviewBoard, String userId, BoardReactionType reaction);

    /** 특정 게시글 + 사용자 + 반응 타입 존재 여부 확인 */
    boolean existsByReviewBoardAndUserIdAndReaction(ReviewBoard reviewBoard, String userId, BoardReactionType reaction);

    /** 특정 게시글 + 사용자 조회 (반응 토글 시 사용) */
    Optional<ReviewBoardReaction> findByReviewBoardAndUserId(ReviewBoard reviewBoard, String userId);

    /** 특정 게시글 관련 모든 반응 삭제 */
    void deleteAllByReviewBoard(ReviewBoard reviewBoard);

    // bno 기준 존재 여부 체크용, 필요시 하나만 사용 가능
    boolean existsByReviewBoard_BnoAndUserIdAndReaction(Long reviewBoardBno, String userId, BoardReactionType reaction);
}