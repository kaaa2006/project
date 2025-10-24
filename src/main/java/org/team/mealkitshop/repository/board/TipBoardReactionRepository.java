package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipBoardReaction;

import java.util.Optional;

/**
 * TipBoardReaction JPA Repository
 * - 팁 게시글(TipBoard)에 대한 사용자(User)의 반응(LIKE / DIS_LIKE)을 조회, 저장, 존재 여부 체크
 */
@Repository
public interface TipBoardReactionRepository extends JpaRepository<TipBoardReaction, Long> {

    /**
     * 특정 팁 게시글과 사용자, 반응 타입 조회
     * @param tipBoard 반응 대상 게시글
     * @param userId 사용자 ID
     * @param reaction 반응 타입(LIKE/DIS_LIKE)
     */
    Optional<TipBoardReaction> findByTipBoardAndUserIdAndReaction(TipBoard tipBoard, String userId, BoardReactionType reaction);

    /** 특정 팁 게시글과 사용자, 반응 타입 존재 여부 확인 */
    boolean existsByTipBoardAndUserIdAndReaction(TipBoard tipBoard, String userId, BoardReactionType reaction);

    /** 특정 팁 게시글과 사용자 조회 (LIKE/디스라이크 상관없이 존재 여부 확인 가능) */
    Optional<TipBoardReaction> findByTipBoardAndUserId(TipBoard tipBoard, String userId);

    /** 특정 팁 게시글과 관련된 모든 TipBoardReaction 삭제 (게시글 삭제 시 연동) */
    void deleteAllByTipBoard(TipBoard tipBoard);

    /** ID 기반 조회용 메서드 (TipBoard 객체 없이 bno로 바로 조회) */
    boolean existsByTipBoard_BnoAndUserIdAndReaction(Long bno, String userId, BoardReactionType reaction);

    /** ✅ 특정 게시글 반응 수 조회 */
    long countByTipBoardBno(Long bno);

    int countByTipBoardAndReaction(TipBoard tipBoard, BoardReactionType reaction);

}