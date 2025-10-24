package org.team.mealkitshop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.service.cart.CartService;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelController {

    private final CartService cartService;

    @ModelAttribute("cartCount")
    public int populateCartCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return 0;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            return cartService.getCartItemCount(user.getMemberId());
        } else if (principal instanceof DefaultOAuth2User oauthUser) {
            Object id = oauthUser.getAttribute("id");
            if (id != null) {
                try {
                    return cartService.getCartItemCount(Long.valueOf(id.toString()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
