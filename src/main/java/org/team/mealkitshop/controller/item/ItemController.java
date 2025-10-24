package org.team.mealkitshop.controller.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.dto.item.ItemDTO;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.ListItemDTO;
import org.team.mealkitshop.service.item.ItemActionService;
import org.team.mealkitshop.service.item.ItemService;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/items", produces = MediaType.APPLICATION_JSON_VALUE)
public class ItemController {

    private final ItemService itemService;
    private final ItemActionService itemActionService;

    /**
     * [사용자용] 상품 목록 조회
     * - 검색조건 + 페이징 지원
     * - 로그인 회원(mno) 있으면 좋아요(liked) 여부 함께 응답
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String foodItem,
            @RequestParam(required = false) Boolean specialDeal,
            @RequestParam(required = false) Boolean newItem,
            @ModelAttribute ItemSearchDTO cond,
            @PageableDefault(size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "mno", required = false) Long mno) {

        if (category != null) { // 대분류 (카테고리별)
            try {
                cond.setCategory(Category.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException e) {
            }
        }

        if (foodItem != null) { // 중분류 (상품 항목별)
            try {
                cond.setFoodItem(org.team.mealkitshop.common.FoodItem.valueOf(foodItem.toUpperCase()));
            } catch (IllegalArgumentException e) { }
        }

        if (specialDeal != null) { // 특가상품 여부 (할인율 50% 이상)
            cond.setSpecialDeal(specialDeal);
        }

        if (newItem != null) {
            cond.setNewItem(newItem);
        }

        Page<ListItemDTO> page = itemService.getListPage(cond, pageable);

        Set<Long> liked = Set.of();
        if (mno != null && !page.isEmpty()) {
            var ids = page.getContent().stream()
                    .map(ListItemDTO::getId)
                    .toList();
            liked = itemActionService.likedItemIds(mno, ids);
        }
        return Map.of("page", page, "likedIds", liked);
    }

    /**
     * [사용자용] 상품 상세 조회
     * - 조회수 증가
     * - 상세 정보 + 리뷰 통계 포함
     * - 회원이 로그인한 경우 liked 상태 포함
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id,
                                      @RequestParam(value = "mno", required = false) Long mno) {
        itemActionService.increaseViewCount(id);
        ItemDTO item = itemService.read(id);
        Map<String, Object> body = new HashMap<>();
        body.put("item", item);
        body.put("avgRating",   Optional.ofNullable(item.getAvgRating()).orElse(0.0)); // 평균 평점
        body.put("reviewCount", Optional.ofNullable(item.getReviewCount()).orElse(0L)); // 총 리뷰 수
        body.put("viewCount",   Optional.ofNullable(item.getItemViewCnt()).orElse(0L)); // 총 조회 수
        //body.put("likeCount",   Optional.ofNullable(item.getItemLike()).orElse(0L)); // 좋아요 누른 수 (해당 코드는 사용 시 주석 해제)

        if (mno != null) {
            body.put("liked", itemActionService.isLiked(id, mno));
            body.put("mno", mno);

        }
        return body;
    }

    /**
     * [사용자용] 좋아요 토글
     * - 이미 눌렀으면 해제, 아니면 좋아요 등록
     *  해당 기능은 미 사용 시 정리예정
     */
    @PostMapping("/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id, @RequestParam Long mno) {
        boolean liked = itemActionService.toggleLike(id, mno);
        return Map.of("itemId", id, "mno", mno, "liked", liked);
    }

    /**
     * [사용자용] 좋아요 상태 조회
     *  해당 기능은 미사용 시 정리예정
     */
    @GetMapping("/{id}/like")
    public Map<String, Object> isLiked(@PathVariable Long id, @RequestParam Long mno) {
        boolean liked = itemActionService.isLiked(id, mno);
        return Map.of("itemId", id, "mno", mno, "liked", liked);
    }
}
