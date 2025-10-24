package org.team.mealkitshop.controller.board;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.dto.board.ReviewBoardReactionDTO;
import org.team.mealkitshop.service.board.ReviewBoardReactionService;

import java.util.HashMap;
import java.util.Map;

/**
 * ReviewBoard 전용 반응 Controller (REST)
 * - '도움 됐어요 / 도움 안 됐어요' 강제 선택 처리
 * - 로그인 회원 확인: Rq 컴포넌트 사용
 */
@RestController
@RequestMapping("/board/reaction/review")
@RequiredArgsConstructor
public class ReviewBoardReactionController {

    private final ReviewBoardReactionService reviewBoardReactionService;
    private final Rq rq;

    @PostMapping(value = "/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> toggleReaction(@RequestParam Long reviewBoardId,
                                            @RequestParam String type) {
        try {
            // 1) 로그인 확인
            if (!rq.isLogined()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "로그인 후 이용 가능합니다."));
            }

            // 2) 요청 타입 변환
            BoardReactionType reactionType =
                    "helpful".equalsIgnoreCase(type)
                            ? BoardReactionType.HELPFUL
                            : BoardReactionType.NOT_HELPFUL;

            // 3) DTO 생성
            ReviewBoardReactionDTO dto = ReviewBoardReactionDTO.builder()
                    .reviewBoardId(reviewBoardId)
                    .userId(rq.getMemberId())
                    .reactionType(reactionType)
                    .build();

            // 4) 토글 처리
            ReviewBoardReactionDTO updatedDto = reviewBoardReactionService.toggleReaction(dto);

            // 5) 사용자 최종 반응 문자열
            String userReaction = null;
            if (updatedDto.isAlreadyAddHelpful()) {
                userReaction = "helpful";
            } else if (updatedDto.isAlreadyAddNotHelpful()) {
                userReaction = "notHelpful";
            }

            // 6) 응답
            Map<String, Object> response = new HashMap<>();
            response.put("helpfulCount", updatedDto.getHelpfulCount());
            response.put("notHelpfulCount", updatedDto.getNotHelpfulCount());
            response.put("userReaction", userReaction);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
