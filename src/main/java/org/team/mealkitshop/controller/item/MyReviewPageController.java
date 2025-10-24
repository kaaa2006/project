package org.team.mealkitshop.controller.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.team.mealkitshop.config.CustomUserDetails;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyReviewPageController {

    @GetMapping("/items/reviews")
    public String myReviewsPage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        model.addAttribute("active", "reviews");
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("myMno", principal.getMemberId());
        // ★ 템플릿 경로 변경
        return "items/review-list";
    }
}
