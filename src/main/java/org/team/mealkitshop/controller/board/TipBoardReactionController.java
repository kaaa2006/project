package org.team.mealkitshop.controller.board;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.dto.board.TipBoardReactionDTO;
import org.team.mealkitshop.service.board.TipBoardReactionService;

import java.util.HashMap;
import java.util.Map;

/**
 * TipBoard 전용 '좋아요 / 싫어요' 컨트롤러
 * - REST API로 TipBoard 반응 토글 처리
 * - 로그인 회원 확인: Rq 컴포넌트 사용
 * - DTO 반환형 기준으로 JSON 반환
 */
@RestController
@RequestMapping("/board/reaction/tip")
@RequiredArgsConstructor
public class TipBoardReactionController {

    private final TipBoardReactionService tipBoardReactionService;
    private final Rq rq;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleReaction(@RequestParam Long tipBoardId,
                                            @RequestParam String type) {

        if (!rq.isLogined()) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인 후 이용 가능합니다."));
        }

        // 'none'이면 해제(null)
        BoardReactionType reactionType = null;
        if ("like".equalsIgnoreCase(type)) reactionType = BoardReactionType.LIKE;
        else if ("dislike".equalsIgnoreCase(type)) reactionType = BoardReactionType.DIS_LIKE;

        TipBoardReactionDTO dto = TipBoardReactionDTO.builder()
                .tipBoardId(tipBoardId)
                .userId(rq.getMemberId())
                .reactionType(reactionType)
                .build();

        TipBoardReactionDTO updatedDto = tipBoardReactionService.toggleReaction(dto);

        // TipBoard DTO에서 바로 userReaction 사용
        String userReaction = updatedDto.getUserReaction(); // "like" / "dislike" / null

        Map<String, Object> response = new HashMap<>();
        response.put("likeCount", updatedDto.getLikeCount());
        response.put("dislikeCount", updatedDto.getDislikeCount());
        response.put("userReaction", userReaction);

        return ResponseEntity.ok(response);
    }
}
