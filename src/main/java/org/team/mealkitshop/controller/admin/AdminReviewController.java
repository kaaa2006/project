package org.team.mealkitshop.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.dto.item.ReviewReplyDTO;
import org.team.mealkitshop.service.item.ReviewReplyService;
import org.team.mealkitshop.service.item.ReviewService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/admin/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final ReviewReplyService replyService;

    /* ===== 관리 목록 조회 ===== */
    @GetMapping
    public ResponseEntity<Page<ReviewDTO>> adminList(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long mno,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (itemId != null) {
            return ResponseEntity.ok(reviewService.listByItem(itemId, pageable, true, true));
        } else if (mno != null) {
            return ResponseEntity.ok(reviewService.listByMember(mno, pageable, true, true));
        }
        return ResponseEntity.ok(Page.empty(pageable));
    }

    /* ===== 관리자 답변 ===== */

    /** 답변 조회 */
    @GetMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewReplyDTO> getReply(@PathVariable Long reviewId) {
        return ResponseEntity.ok(replyService.getByReviewId(reviewId));
    }

    /** 답변 생성 */
    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewReplyDTO> createReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReviewReplyDTO.Write body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(replyService.create(reviewId, principal.getMemberId(), body.getContent()));
    }

    /** 답변 수정 */
    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewReplyDTO> updateReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReviewReplyDTO.Write body
    ) {
        return ResponseEntity.ok(replyService.update(reviewId, principal.getMemberId(), body.getContent()));
    }

    /** 답변 삭제 */
    @DeleteMapping("/{reviewId}/reply")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        replyService.delete(reviewId, principal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    /* ===== 리뷰 강제 삭제(관리자) ===== */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.delete(reviewId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
