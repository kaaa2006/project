package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.BoardReactionType;

/**
 * TipBoard 전용 반응 DTO
 * - '좋아요 / 싫어요' 관리용
 * - UI 상태/카운트 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipBoardReactionDTO {

    private Long tipBoardId;          // TipBoard 게시글 ID
    private String userId;            // 사용자 ID
    private BoardReactionType reactionType; // LIKE / DIS_LIKE 상태

    // ✅ 서버에서 반환할 UI용 필드
    private int likeCount;              // 현재 좋아요 수
    private int dislikeCount;           // 현재 싫어요 수

    private String userReaction; // 좋아요/싫어요 상태: "like", "dislike", null

}
