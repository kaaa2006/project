package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.BoardReactionType;

/**
 * ReviewBoard 전용 반응 DTO
 * - '도움 됐어요 / 도움 안 됐어요' 관리용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewBoardReactionDTO {

    private Long reviewBoardId;       // ReviewBoard 게시글 ID

    private String userId;            // 반응한 사용자 ID

    private BoardReactionType reactionType; // HELPUL / NOT_HELPFUL 상태

    // ✅ 서버에서 반환할 UI용 필드
    private int helpfulCount;             // 현재 도움 됐어요 수
    private int notHelpfulCount;          // 현재 도움 안 됐어요 수
    private boolean isAlreadyAddHelpful;  // 로그인 사용자가 이미 도움 됐어요 클릭했는지
    private boolean isAlreadyAddNotHelpful; // 로그인 사용자가 이미 도움 안 됐어요 클릭했는지

}
