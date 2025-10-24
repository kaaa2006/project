package org.team.mealkitshop.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.dto.cart.*;
import org.team.mealkitshop.service.cart.CartService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    private Long extractMemberId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getMemberId();
        } else if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oauthUser = (DefaultOAuth2User) principal;
            Object id = oauthUser.getAttribute("id");
            if (id != null) {
                try {
                    return Long.valueOf(id.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /** ✅ 장바구니 조회 (JSON 응답, zipcode는 선택사항) */
    @GetMapping
    public ResponseEntity<CartDetailResponse> viewCart(Authentication authentication,
                                                       @RequestParam(required = false) String zipcode) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        CartDetailResponse cart = cartService.getCartDetail(memberId, zipcode);
        return ResponseEntity.ok(cart);
    }

    /** ✅ 장바구니 상품 추가 (AJAX POST) */
    @PostMapping("/add")
    public ResponseEntity<Long> addToCart(Authentication authentication,
                                          @RequestBody AddToCartRequest request) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        Long addedCartItemId = cartService.addToCart(memberId, request);
        return ResponseEntity.ok(addedCartItemId);
    }

    /** ✅ 장바구니 항목 수정 (AJAX PUT) */
    @PutMapping("/update")
    public ResponseEntity<Void> updateCartItem(Authentication authentication,
                                               @RequestBody UpdateCartItemRequest request) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        cartService.updateCartItem(memberId, request);
        return ResponseEntity.ok().build();
    }

    /** ✅ 장바구니 전체 비우기 (AJAX DELETE) */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        cartService.clearCart(memberId);
        return ResponseEntity.ok().build();
    }

    /** ✅ 선택항목 삭제 */
    @DeleteMapping("/checked")
    public ResponseEntity<Void> deleteCheckedItems(Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        cartService.deleteCheckedItems(memberId);
        return ResponseEntity.ok().build();
    }

    /** ✅ 선택항목 주문 (상품 리스트 조회) */
    @GetMapping("/checked-items")
    public ResponseEntity<List<CartItemDto>> getCheckedCartItems(Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build(); // 인증 실패 시 401 반환
        }

        List<CartItemDto> checkedItems = cartService.getCheckedCartItems(memberId);
        return ResponseEntity.ok(checkedItems);
    }

    /** ✅ 선택항목 주문 요약 (zipcode 반영) - 🚨 새로 추가된 API */
    @PostMapping("/checked-summary")
    public ResponseEntity<CartDetailResponse> getCheckedSummary(
            Authentication authentication,
            @RequestBody CheckedSummaryRequest request
    ) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        CartDetailResponse summary = cartService.getSelectedCartDetail(
                memberId,
                request.getCartItemIds(),
                request.getZipcode()
        );
        return ResponseEntity.ok(summary);
    }
}
