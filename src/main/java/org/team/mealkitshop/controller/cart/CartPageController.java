package org.team.mealkitshop.controller.cart;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cart")
public class CartPageController {

    @GetMapping("/view")
    public String showCartPage() {
        return "cart/view"; // resources/templates/cart-view.html 렌더링
    }
}
