package org.team.mealkitshop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.service.admin.AdminOrderService;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService orderService;

    /** 주문 목록 */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) Pay pay,
                       Model model) {

        Page<Order> orderPage = orderService.getOrders(page, size, sort, status, pay);

        // ✅ 테이블 데이터
        model.addAttribute("orders", orderPage.getContent());

        // ✅ 페이지네이션용
        model.addAttribute("page", orderPage);

        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pays", Pay.values());
        model.addAttribute("status", status);
        model.addAttribute("pay", pay);
        model.addAttribute("sort", sort);

        return "admin/orders/list";
    }


    /** 상태 변경 */
    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId,
                               @RequestParam OrderStatus status,
                               @RequestParam(defaultValue = "0") int page) {
        orderService.updateStatus(orderId, status);
        return "redirect:/admin/orders?page=" + page;
    }
}