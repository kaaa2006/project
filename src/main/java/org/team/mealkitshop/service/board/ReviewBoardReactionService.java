package org.team.mealkitshop.service.board;

import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.dto.board.ReviewBoardReactionDTO;

/**
 * 리뷰 게시판 전용 '도움 됐어요 / 안 됐어요' 서비스 인터페이스
 */
public interface ReviewBoardReactionService {

    /** 도움 됐어요 / 안 됐어요 토글 처리 */
    ReviewBoardReactionDTO toggleReaction(ReviewBoardReactionDTO dto);

    /** 특정 게시글에 이미 도움 됐어요 클릭했는지 확인 */
    boolean isAlreadyAddGoodRp(long reviewBoardId, String userId);

    /** 특정 게시글에 이미 도움 안 됐어요 클릭했는지 확인 */
    boolean isAlreadyAddBadRp(long reviewBoardId, String userId);

    /** 특정 게시글의 도움 됐어요 수 */
    int getHelpfulCount(long reviewBoardId);

    /** 특정 게시글의 도움 안 됐어요 수 */
    int getNotHelpfulCount(long reviewBoardId);

    /** ✅ 로그인 사용자가 게시글에 누른 반응 상태 반환 ("helpful", "notHelpful", null) */
    default String getUserReaction(String userId, Long reviewBoardId) {
        if (isAlreadyAddGoodRp(reviewBoardId, userId)) return "helpful";
        if (isAlreadyAddBadRp(reviewBoardId, userId)) return "notHelpful";
        return null;
    }

}