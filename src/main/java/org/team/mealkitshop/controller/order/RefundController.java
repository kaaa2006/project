package org.team.mealkitshop.controller.order;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.common.RefundReason;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.dto.checkout.RefundListDTO;
import org.team.mealkitshop.service.order.OrderService;

import java.util.List;

@Controller
@RequestMapping("/orders") // ✅ /orders 하위로 정리
@RequiredArgsConstructor
public class RefundController {

    private final OrderService orderService;

    /** 환불 요청 생성 */
    @PostMapping("/{id}/refund")
    public String requestRefund(@PathVariable("id") Long orderId,
                                @AuthenticationPrincipal CustomUserDetails user,
                                @RequestParam("reason") RefundReason reason,
                                @RequestParam(value = "reasonDetail", required = false) String reasonDetail,
                                RedirectAttributes redirectAttributes) {

        Long memberId = user.getMemberId();
        orderService.requestRefund(orderId, memberId, reason, reasonDetail);

        // ✅ RedirectAttributes 사용 → 자동 URL 인코딩
        redirectAttributes.addAttribute("msg", "환불요청이 접수되었습니다.");
        return "redirect:/orders/{id}/detail";
    }

    /** 환불/취소 내역 조회 */
    @GetMapping("/refunds")
    public String refundList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long memberId = user.getMemberId();
        List<RefundListDTO> refunds = orderService.getRefundAndCanceledOrders(memberId);
        model.addAttribute("refunds", refunds);
        return "refunds/refund-list"; // ✅ templates/refunds/refund-list.html
    }
}
