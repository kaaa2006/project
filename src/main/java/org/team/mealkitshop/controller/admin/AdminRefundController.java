package org.team.mealkitshop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.common.RefundStatus;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.domain.order.OrderRefund;
import org.team.mealkitshop.service.admin.AdminRefundService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    /** 환불 요청 목록 조회 */
    @GetMapping
    public String listRefunds(@RequestParam(value = "status", required = false) RefundStatus status,
                              Pageable pageable,
                              Model model) {
        Page<OrderRefund> refunds;
        if (status != null) {
            refunds = adminRefundService.getRefundRequestsByStatus(status, pageable);
        } else {
            refunds = adminRefundService.getRefundRequests(pageable);
        }

        model.addAttribute("refunds", refunds);
        model.addAttribute("status", status);

        return "admin/refunds/list"; // → templates/admin/refunds/list.html
    }

    /** 환불 승인 */
    @PostMapping("/{id}/approve")
    public String approveRefund(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails admin) {
        adminRefundService.approveRefund(id, admin.getMemberName()); // ✅ 로그인 이름 저장
        return "redirect:/admin/refunds";
    }

    /** 환불 거절 */
    @PostMapping("/{id}/reject")
    public String rejectRefund(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails admin) {
        adminRefundService.rejectRefund(id, admin.getMemberName()); // ✅ 로그인 이름 저장
        return "redirect:/admin/refunds";
    }
}
