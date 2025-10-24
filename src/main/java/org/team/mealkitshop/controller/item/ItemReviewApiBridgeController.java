package org.team.mealkitshop.controller.item;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.service.item.ReviewService;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/items", produces = MediaType.APPLICATION_JSON_VALUE)
public class ItemReviewApiBridgeController {

    private final ReviewService reviewService;

    /** JS에서 호출하는 경로를 그대로 받는 얇은 어댑터 */
    @PermitAll
    @GetMapping("/{itemId}/reviews")
    public ResponseEntity<Page<ReviewDTO>> listByItemForApi(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean withImages,
            @RequestParam(defaultValue = "true")  boolean withReply,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.listByItem(itemId, pageable, withImages, withReply));
    }
}