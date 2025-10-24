package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReaction;
import org.team.mealkitshop.dto.board.ReviewBoardReactionDTO;
import org.team.mealkitshop.repository.board.BoardRepository;
import org.team.mealkitshop.repository.board.ReviewBoardReactionRepository;
import org.team.mealkitshop.repository.board.ReviewBoardRepository;

import java.util.Optional;

/**
 * 리뷰 게시판 '도움 됐어요 / 안 됐어요' 서비스 구현체
 * - 사용자 단위 토글
 * - 동일 반응 클릭 시 해제
 * - 반대 반응 클릭 시 기존 반응 삭제 후 새 반응 추가
 * - ReviewBoard의 helpfulCount / notHelpfulCount 자동 업데이트
 */
@Service
@RequiredArgsConstructor
public class ReviewBoardReactionServiceImpl implements ReviewBoardReactionService {

    private final ReviewBoardReactionRepository reactionRepository;
    private final ReviewBoardRepository reviewBoardRepository;

    /**
     * 토글 처리 (동일 버튼 클릭 시 해제, 반대 버튼 클릭 시 전환)
     * @return 현재 게시글 상태와 count DTO
     */
    @Transactional
    @Override
    public ReviewBoardReactionDTO toggleReaction(ReviewBoardReactionDTO dto) {
        // 1. 게시글 조회
        ReviewBoard board = reviewBoardRepository.findById(dto.getReviewBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        String userId = dto.getUserId();
        BoardReactionType clickedType = dto.getReactionType();
        BoardReactionType oppositeType = (clickedType == BoardReactionType.HELPFUL)
                ? BoardReactionType.NOT_HELPFUL
                : BoardReactionType.HELPFUL;

        // 2. 사용자의 기존 반응 체크
        Optional<ReviewBoardReaction> existingClicked = reactionRepository.findByReviewBoardAndUserIdAndReaction(board, userId, clickedType);
        Optional<ReviewBoardReaction> existingOpposite = reactionRepository.findByReviewBoardAndUserIdAndReaction(board, userId, oppositeType);

        if (existingClicked.isPresent()) {
            // 동일 버튼 클릭 → 삭제
            reactionRepository.delete(existingClicked.get());
            reactionRepository.flush(); // 🔥 DB에 즉시 반영
            updateCount(board, clickedType, -1);
        } else {
            // 반대 버튼 있으면 삭제
            if (existingOpposite.isPresent()) {
                reactionRepository.delete(existingOpposite.get());
                reactionRepository.flush(); // 🔥 DB에 즉시 반영
                updateCount(board, oppositeType, -1);
            }

            // 새 반응 추가
            ReviewBoardReaction newReaction = ReviewBoardReaction.builder()
                    .reviewBoard(board)
                    .userId(userId)
                    .reaction(clickedType)
                    .build();
            reactionRepository.save(newReaction);
            updateCount(board, clickedType, +1);
        }

        // ✅ 카운트 최신화 위해 DB에서 다시 조회
        ReviewBoard refreshedBoard = reviewBoardRepository.findById(board.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

// 6. 최종 상태 DTO 반환
        boolean finalHelpful = reactionRepository.existsByReviewBoardAndUserIdAndReaction(refreshedBoard, userId, BoardReactionType.HELPFUL);
        boolean finalNotHelpful = reactionRepository.existsByReviewBoardAndUserIdAndReaction(refreshedBoard, userId, BoardReactionType.NOT_HELPFUL);

        return ReviewBoardReactionDTO.builder()
                .reviewBoardId(refreshedBoard.getBno())
                .helpfulCount(refreshedBoard.getHelpfulCount())   // ✅ 새로 조회한 값
                .notHelpfulCount(refreshedBoard.getNotHelpfulCount()) // ✅ 새로 조회한 값
                .isAlreadyAddHelpful(finalHelpful)
                .isAlreadyAddNotHelpful(finalNotHelpful)
                .build();
    }

    @Override
    public boolean isAlreadyAddGoodRp(long reviewBoardId, String userId) {
        ReviewBoard board = reviewBoardRepository.findById(reviewBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return reactionRepository.existsByReviewBoardAndUserIdAndReaction(board, userId, BoardReactionType.HELPFUL);
    }

    @Override
    public boolean isAlreadyAddBadRp(long reviewBoardId, String userId) {
        ReviewBoard board = reviewBoardRepository.findById(reviewBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return reactionRepository.existsByReviewBoardAndUserIdAndReaction(board, userId, BoardReactionType.NOT_HELPFUL);
    }

    /** 도움 됐어요 / 안 됐어요 카운트 업데이트 */
    private void updateCount(ReviewBoard board, BoardReactionType type, int delta) {
        if (type == BoardReactionType.HELPFUL) {
            reviewBoardRepository.updateHelpfulCount(board.getBno(), delta);
        } else {
            reviewBoardRepository.updateNotHelpfulCount(board.getBno(), delta);
        }
    }

    @Override
    public int getHelpfulCount(long reviewBoardId) {
        ReviewBoard board = reviewBoardRepository.findById(reviewBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return board.getHelpfulCount();
    }

    @Override
    public int getNotHelpfulCount(long reviewBoardId) {
        ReviewBoard board = reviewBoardRepository.findById(reviewBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return board.getNotHelpfulCount();
    }
}