package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipBoardReaction;
import org.team.mealkitshop.dto.board.TipBoardReactionDTO;
import org.team.mealkitshop.repository.board.TipBoardReactionRepository;
import org.team.mealkitshop.repository.board.TipBoardRepository;

import java.util.Optional;

/**
 * 팁 게시판 '좋아요 / 싫어요' 전용 서비스 구현체
 * - 사용자 단위 토글
 * - 동일 반응이면 해제, 반대 반응이면 변경
 * - TipBoard의 likeCount / dislikeCount 자동 업데이트
 * - UI 상태/카운트를 포함한 DTO 반환
 */
@Service
@RequiredArgsConstructor
public class TipBoardReactionServiceImpl implements TipBoardReactionService {

    private final TipBoardReactionRepository reactionRepository;
    private final TipBoardRepository tipBoardRepository;

    /**
     * 좋아요 / 싫어요 토글 처리
     */
    @Transactional
    @Override
    public TipBoardReactionDTO toggleReaction(TipBoardReactionDTO dto) {
        TipBoard tipBoard = tipBoardRepository.findById(dto.getTipBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        String userId = dto.getUserId();
        BoardReactionType clickedType = dto.getReactionType(); // null이면 해제 요청

        // === 1) 해제 요청 ===
        if (clickedType == null) {
            reactionRepository.findByTipBoardAndUserIdAndReaction(tipBoard, userId, BoardReactionType.LIKE)
                    .ifPresent(r -> { reactionRepository.delete(r); updateCount(tipBoard, BoardReactionType.LIKE, -1); });

            reactionRepository.findByTipBoardAndUserIdAndReaction(tipBoard, userId, BoardReactionType.DIS_LIKE)
                    .ifPresent(r -> { reactionRepository.delete(r); updateCount(tipBoard, BoardReactionType.DIS_LIKE, -1); });
        }

        // === 2) 반응 클릭 ===
        else {
            Optional<TipBoardReaction> existingClicked =
                    reactionRepository.findByTipBoardAndUserIdAndReaction(tipBoard, userId, clickedType);

            if (existingClicked.isPresent()) {
                // ✅ 같은 버튼 다시 누른 경우 → 해제
                reactionRepository.delete(existingClicked.get());
                reactionRepository.flush();
                updateCount(tipBoard, clickedType, -1);
            }
            else {
                // ✅ 반대 버튼 눌렀을 경우 → 반대 반응 제거 후 새 반응 추가
                BoardReactionType oppositeType = (clickedType == BoardReactionType.LIKE)
                        ? BoardReactionType.DIS_LIKE : BoardReactionType.LIKE;

                reactionRepository.findByTipBoardAndUserIdAndReaction(tipBoard, userId, oppositeType)
                        .ifPresent(r -> {
                            reactionRepository.delete(r);
                            reactionRepository.flush();
                            updateCount(tipBoard, oppositeType, -1);
                        });

                TipBoardReaction newReaction = TipBoardReaction.builder()
                        .tipBoard(tipBoard)
                        .userId(userId)
                        .reaction(clickedType)
                        .build();
                reactionRepository.save(newReaction);
                reactionRepository.flush();
                updateCount(tipBoard, clickedType, +1);
            }
        }

        // === 3) 최신 상태 DTO 반환 ===
        TipBoard refreshed = tipBoardRepository.findById(tipBoard.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        String userReaction = null;
        if (reactionRepository.existsByTipBoardAndUserIdAndReaction(refreshed, userId, BoardReactionType.LIKE))
            userReaction = "like";
        else if (reactionRepository.existsByTipBoardAndUserIdAndReaction(refreshed, userId, BoardReactionType.DIS_LIKE))
            userReaction = "dislike";

        return TipBoardReactionDTO.builder()
                .tipBoardId(refreshed.getBno())
                .likeCount(refreshed.getLikeCount())
                .dislikeCount(refreshed.getDislikeCount())
                .userReaction(userReaction)
                .build();
    }

    /** 좋아요/싫어요 카운트 업데이트 */
    private void updateCount(TipBoard tipBoard, BoardReactionType type, int value) {
        if (type == BoardReactionType.LIKE) {
            if (value > 0) tipBoard.increaseLike();
            else tipBoard.decreaseLike();
        } else {
            if (value > 0) tipBoard.increaseDislike();
            else tipBoard.decreaseDislike();
        }
        tipBoardRepository.save(tipBoard);
    }

    @Override
    public boolean isAlreadyAddGoodRp(long tipBoardId, String userId) {
        TipBoard tipBoard = tipBoardRepository.findById(tipBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return reactionRepository.existsByTipBoardAndUserIdAndReaction(tipBoard, userId, BoardReactionType.LIKE);
    }

    @Override
    public boolean isAlreadyAddBadRp(long tipBoardId, String userId) {
        TipBoard tipBoard = tipBoardRepository.findById(tipBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return reactionRepository.existsByTipBoardAndUserIdAndReaction(tipBoard, userId, BoardReactionType.DIS_LIKE);
    }
}