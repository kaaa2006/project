package org.team.mealkitshop.controller.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.cart.CartDetailResponse;
import org.team.mealkitshop.dto.checkout.OrderDetailResponse;
import org.team.mealkitshop.dto.payment.PgCallbackRequest;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.service.cart.CartService;
import org.team.mealkitshop.service.order.OrderService;

import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders") // ✅ 모든 주문 관련 URL prefix
@Log4j2
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;

    /* ===================== 주문 목록 ===================== */
    @GetMapping("/list")
    public String listOrders(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long memberMno = user.getMemberId();

        // ✅ 이미 DTO 반환
        List<OrderDetailResponse> orders = orderService.getOrders(memberMno);

        log.info(">>> 주문 목록 조회: memberMno={}, orders size={}", memberMno, orders.size());
        model.addAttribute("orders", orders);

        return "order/list"; // ✅ Thymeleaf: src/main/resources/templates/order/list.html
    }

    /* ===================== 주문 상세 ===================== */
    @Transactional(readOnly = true)
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              @AuthenticationPrincipal CustomUserDetails user,
                              Model model) {
        Long memberMno = user.getMemberId();
        var order = orderService.getOrderDetail(orderId, memberMno); // ✅ 이미 DTO
        model.addAttribute("order", order);

        return "order/order-detail"; // ✅ 파일명에 맞춤
    }

    /* ===================== 체크아웃 ===================== */
    @GetMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserDetails user,
                           @RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds,
                           Model model) {
        Long memberId = user.getMemberId();

        CartDetailResponse cartDetail;
        if (cartItemIds != null && !cartItemIds.isEmpty()) {
            cartDetail = cartService.getSelectedCartDetail(memberId, cartItemIds);
        } else {
            cartDetail = cartService.getCartDetail(memberId, "00000");
        }
        model.addAttribute("cart", cartDetail);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + memberId));
        model.addAttribute("member", member);

        List<Address> addresses = addressRepository.findByMember(member);
        model.addAttribute("addresses", addresses);

        return "order/checkout";
    }

    @PostMapping("/checkout")
    public String submitCheckout(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds,
                                 @RequestParam(value = "addressId", required = false) Long addressId,
                                 @RequestParam("payMethod") Pay payMethod,
                                 @RequestParam(value = "selectedOnly", defaultValue = "false") boolean selectedOnly) {
        Long memberId = user.getMemberId();

        if (addressId == null) {
            throw new IllegalStateException("배송지를 선택해야 합니다.");
        }

        Long orderId;
        if (selectedOnly) {
            if (cartItemIds == null || cartItemIds.isEmpty()) {
                throw new IllegalStateException("선택 주문 정보가 유실되었습니다. 다시 시도해주세요.");
            }
            orderId = orderService.createSelectedOrder(memberId, cartItemIds, addressId, payMethod);
        } else if (cartItemIds != null && !cartItemIds.isEmpty()) {
            orderId = orderService.createSelectedOrder(memberId, cartItemIds, addressId, payMethod);
        } else {
            orderId = orderService.createOrder(memberId, addressId, payMethod);
        }

        return "redirect:/orders/complete/" + orderId;
    }

    /* ===================== PG 결제 ===================== */
    @GetMapping("/payments/pg-start")
    public String startPayment(@RequestParam("orderId") Long orderId, Model model) {
        var order = orderService.getOrder(orderId); // ✅ 이미 DTO
        model.addAttribute("order", order);
        model.addAttribute("pgClientKey", "PG사에서 발급받은 키");
        return "payment/pgstart";
    }

    @PostMapping("/payments/pg-callback")
    public ResponseEntity<String> pgCallback(@RequestBody PgCallbackRequest request) {
        if (request.getOrderId() == null) {
            return ResponseEntity.badRequest().body("orderId 누락");
        }

        var order = orderService.getOrder(request.getOrderId()); // ✅ 이미 DTO

        if (!Objects.equals(order.getPayableAmount(), request.getAmount())) {
            log.error("PG Callback 금액 불일치: orderId={}, expected={}, actual={}",
                    order.getOrderId(), order.getPayableAmount(), request.getAmount());
            return ResponseEntity.badRequest().body("금액 불일치");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            return ResponseEntity.badRequest().body("잘못된 주문 상태");
        }
        if (order.getPayMethod() == Pay.BANK_TRANSFER) {
            return ResponseEntity.badRequest().body("무통장은 PG 콜백 불가");
        }

        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PREPARING);
        return ResponseEntity.ok("{\"result\":\"success\"}");
    }

    /* ===================== 무통장 입금 ===================== */
    @GetMapping("/{orderId}/transfer")
    public String bankTransferPage(@PathVariable Long orderId,
                                   @AuthenticationPrincipal CustomUserDetails user,
                                   Model model) {
        Long memberMno = user.getMemberId();
        var order = orderService.getOrderDetail(orderId, memberMno); // ✅ 이미 DTO

        model.addAttribute("order", order);
        model.addAttribute("bankAccount", "우리은행 123-456-7890");
        return "payment/transfer";
    }

    @PostMapping("/{orderId}/confirm")
    public String confirmPayment(@PathVariable Long orderId,
                                 @AuthenticationPrincipal CustomUserDetails user) {
        orderService.confirmPayment(orderId);
        return "redirect:/orders/" + orderId;
    }

    /* ===================== 상태 업데이트 ===================== */
    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId,
                               @RequestParam("status") OrderStatus status) {
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/orders/" + orderId;
    }

    /* ===================== 배송지 ===================== */
    @PostMapping("/address")
    public String addAddress(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam String alias,
                             @RequestParam String zipCode,
                             @RequestParam String addr1,
                             @RequestParam(required = false) String addr2) {
        Long memberId = user.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Address address = new Address();
        address.setMember(member);
        address.setAlias(alias);
        address.setZipCode(zipCode);
        address.setAddr1(addr1);
        address.setAddr2(addr2);
        address.setDefault(false);

        addressRepository.save(address);
        return "redirect:/orders/checkout";
    }

    /* ===================== 주문 완료 ===================== */
    /* ===================== 주문 완료 ===================== */
    @GetMapping("/complete/{orderId}")
    public String orderComplete(@PathVariable Long orderId,
                                @AuthenticationPrincipal CustomUserDetails user,
                                Model model) {
        Long memberMno = user.getMemberId();

        // ✅ 주문 상세 (이미 DTO)
        var order = orderService.getOrderDetail(orderId, memberMno);
        model.addAttribute("order", order);

        // ✅ 주문자 정보
        model.addAttribute("memberName", order.getReceiverName());
        model.addAttribute("memberNum", order.getMemberMno());
        model.addAttribute("memberPhone", order.getReceiverPhone());

        // ✅ 잔여 포인트 추가
        Member member = memberRepository.findById(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + memberMno));
        model.addAttribute("memberPoints", member.getPoints());

        // ✅ 장바구니 카운트
        int cartCount = cartService.getCartItemCount(memberMno);
        model.addAttribute("cartCount", cartCount);

        return "order/complete";
    }



    /* ===================== 주문 상세 (영수증 스타일) ===================== */
    @Transactional(readOnly = true)
    @GetMapping("/{orderId}/detail")
    public String orderDetailPage(@PathVariable Long orderId,
                                  @AuthenticationPrincipal CustomUserDetails user,
                                  Model model) {
        Long memberId = user.getMemberId();

        // 서비스에서 DTO 가져오기
        var order = orderService.getOrderDetail(orderId, memberId);
        model.addAttribute("order", order);

        // 주문자 정보 추가 (영수증 헤더용)
        model.addAttribute("memberName", order.getReceiverName());
        model.addAttribute("memberPhone", order.getReceiverPhone());

        // ✅ 파일이 templates/order/detail.html 이라면 이렇게 맞추기
        return "order/order-detail";
// → src/main/resources/templates/order/order-detail.html

    }
    /* ===================== 주문 취소 ===================== */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId,
                              @AuthenticationPrincipal CustomUserDetails user) {
        Long memberId = user.getMemberId();

        try {
            orderService.cancelOrder(orderId, memberId);
        } catch (IllegalStateException e) {
            log.error("주문 취소 실패: {}", e.getMessage());
            return "redirect:/orders/" + orderId + "?error=" + e.getMessage();
        }

        return "redirect:/orders/" + orderId;
    }


}
