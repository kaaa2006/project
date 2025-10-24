package org.team.mealkitshop.controller.item;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.service.item.ReviewImageService;
import org.team.mealkitshop.service.item.ReviewService;

import java.util.List;
import java.util.Map;

@PreAuthorize("isAuthenticated()") // 기본 로그인 필요
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewImageService reviewImageService;

    /* ==================== READ ==================== */

    /** 아이템별 리뷰 목록 조회 */
    @PermitAll
    @GetMapping("/items/{itemId}")
    public ResponseEntity<Page<ReviewDTO>> listByItem(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean withImages,
            @RequestParam(defaultValue = "true") boolean withReply,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable // CHANGED
    ) {
        return ResponseEntity.ok(reviewService.listByItem(itemId, pageable, withImages, withReply));
    }

    /** 회원별 본인이 작성한 리뷰 목록 조회 */
    @GetMapping("/my")
    public ResponseEntity<Page<ReviewDTO>> listMyReviews(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "true") boolean withImages,
            @RequestParam(defaultValue = "true") boolean withReply,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long myMno = principal.getMemberId();
        return ResponseEntity.ok(reviewService.listByMember(myMno, pageable, withImages, withReply));
    }

    /** 리뷰 단건 상세 조회 */
    @PermitAll
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> detail(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "true") boolean withImages,
            @RequestParam(defaultValue = "true") boolean withReply
    ) {
        return ResponseEntity.ok(reviewService.getDetail(reviewId, withImages, withReply));
    }

    /* ==================== CREATE ==================== */

    /** 리뷰 작성 (multipart/form-data) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute ReviewWriteForm form,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        // 작성자 스푸핑 방지
        if (!principal.getMemberId().equals(form.getWriterMno())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "작성자 불일치"));
        }
        ReviewDTO dto = ReviewDTO.builder()
                .itemId(form.getItemId())
                .writerMno(form.getWriterMno())
                .rating(form.getRating())
                .content(form.getContent())
                .build();
        Long newId = reviewService.create(dto, images == null ? List.of() : images);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", newId));
    }

    /* ==================== UPDATE ==================== */

    /** 리뷰 수정 (본문/평점/이미지 교체 옵션) */
    @PatchMapping(path = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute ReviewPatchForm form,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        if (!principal.getMemberId().equals(form.getWriterMno())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ReviewDTO patch = ReviewDTO.builder()
                .id(reviewId)
                .writerMno(form.getWriterMno())
                .rating(form.getRating())
                .content(form.getContent())
                .build();
        reviewService.update(patch, images == null ? List.of() : images, form.isReplaceImages(), principal.getMemberId()); // CHANGED
        return ResponseEntity.noContent().build();
    }

    /* ==================== IMAGE 관리 ==================== */

    /** 리뷰 이미지 추가 */
    @PostMapping(path = "/{reviewId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addImages(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ensureOwner(reviewId, principal.getMemberId()); // NEW
        reviewImageService.addImages(reviewId, images == null ? List.of() : images);
        return ResponseEntity.noContent().build();
    }

    /** 리뷰 이미지 일괄 교체 */
    @PutMapping(path = "/{reviewId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replaceImages(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ensureOwner(reviewId, principal.getMemberId());
        reviewImageService.replaceImages(reviewId, images == null ? List.of() : images);
        return ResponseEntity.noContent().build();
    }


    /** 리뷰 이미지 단건 삭제 */
    @DeleteMapping("/{reviewId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long reviewId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        ensureOwner(reviewId, principal.getMemberId());
        reviewImageService.deleteImage(imageId, reviewId);
        return ResponseEntity.noContent().build();
    }

    /** 리뷰별 이미지 목록 조회 */
    @GetMapping("/{reviewId}/images")
    public ResponseEntity<List<ReviewImageDTO>> imagesByReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewImageService.listByReview(reviewId));
    }

    /* ==================== DELETE ==================== */

    /** 리뷰 삭제 (이미지/답변 포함) */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        ensureOwner(reviewId, principal.getMemberId()); // NEW
        reviewService.delete(reviewId);
        return ResponseEntity.noContent().build();
    }


    /* ==================== HELPER ==================== */
    private void ensureOwner(Long reviewId, Long myMno) {
        Long owner = reviewService.getOwnerMno(reviewId); // fetch-join 기반
        if (!java.util.Objects.equals(owner, myMno)) {
            throw new SecurityException("리뷰 수정/이미지 관리 권한이 없습니다.");
        }
    }

    /* ==================== Form DTO ==================== */

    @Getter @Setter
    public static class ReviewWriteForm {
        @NotNull private Long itemId;
        @NotNull private Long writerMno;
        @Min(1) @Max(5) private int rating;
        @NotBlank @jakarta.validation.constraints.Size(max = 2000) private String content;
    }

    @Getter @Setter
    public static class ReviewPatchForm {
        @NotNull private Long writerMno;
        @Min(1) @Max(5) private int rating;
        @NotBlank @jakarta.validation.constraints.Size(max = 2000) private String content;
        private boolean replaceImages = false;
    }

    /* ==================== Exception Handling ==================== */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }


}
