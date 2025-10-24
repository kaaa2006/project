package org.team.mealkitshop.service.order;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.common.Pay;
import org.team.mealkitshop.common.RefundReason;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderItem;
import org.team.mealkitshop.domain.order.OrderRefund;
import org.team.mealkitshop.dto.checkout.OrderDetailResponse;
import org.team.mealkitshop.dto.checkout.RefundListDTO;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.repository.order.OrderRefundRepository;
import org.team.mealkitshop.repository.order.OrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRefundRepository orderRefundRepository;

    /** ✅ 회원의 환불/취소 내역 가져오기 */
    @Transactional(readOnly = true)
    public List<RefundListDTO> getRefundAndCanceledOrders(Long memberId) {
        return orderRepository.findByMember_MnoAndStatusIn(
                        memberId,
                        List.of(OrderStatus.REFUNDED, OrderStatus.CANCELED)
                )
                .stream()
                .map(RefundListDTO::from)
                .toList();
    }


    public Long createOrder(Long memberId, Pay payMethod) {
        return createOrderInternal(memberId, null, payMethod);
    }

    public Long createOrder(Long memberId, Long addressId, Pay payMethod) {
        return createOrderInternal(memberId, addressId, payMethod);
    }

    private Long createOrderInternal(Long memberId, Long addressId, Pay payMethod) {
        Member member = getMember(memberId);
        Cart cart = getCart(memberId);
        List<CartItem> cartItems = getCartItems(cart);

        Address address = (addressId != null)
                ? addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지 없음: " + addressId))
                : addressRepository.findFirstByMember_MnoAndIsDefaultTrue(member.getMno())
                .orElseThrow(() -> new IllegalStateException("배송지를 선택하거나 기본 배송지를 설정해야 합니다."));

        Order order = buildOrder(member, cartItems, address, payMethod);
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(cartItems);
        cartItemRepository.flush();
        cart.getItems().clear();

        return savedOrder.getOrderId();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + memberId));
    }

    private Cart getCart(Long memberId) {
        return cartRepository.findByMember_Mno(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 없음: " + memberId));
    }

    private List<CartItem> getCartItems(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어있음");
        }
        return cartItems;
    }

    private Order buildOrder(Member member, List<CartItem> cartItems, Address address, Pay payMethod) {
        String orderNo = generateOrderNo();

        Order order = Order.builder()
                .orderNo(orderNo)
                .member(member)
                .orderDate(LocalDateTime.now())
                .payMethod(payMethod)
                .receiverName(member.getMemberName())
                .receiverPhone(member.getPhone())
                .address(address)
                .status(OrderStatus.CREATED)
                .discountTotal(0)
                .build();

        int total = 0;
        for (CartItem ci : cartItems) {
            var item = ci.getItem();
            int qty = ci.getQuantity();

            item.decreaseStock(qty);
            OrderItem oi = OrderItem.of(item, qty);
            order.addItem(oi);

            total += ci.getLinePayable(); // ✅ 할인 기준: 세일가 × 수량
        }
        order.setProductsTotal(total); // 실제 주문 금액 기준 설정

        int percent = switch (member.getGrade()) {
            case VIP -> 10;
            case GOLD -> 7;
            case SILVER -> 5;
            default -> 0;
        };

        int discount = Math.min((int) Math.round(total * (percent / 100.0)), total);
        order.setDiscountTotal(discount);

        int afterDiscount = total - discount;
        int shippingFee = 0;
        if (afterDiscount < 50_000 && afterDiscount > 0) shippingFee += 3_000;
        String zip = (address != null ? address.getZipCode() : null);
        if (zip != null && zip.startsWith("63")) shippingFee += 5_000;
        if (member.getGrade() == Grade.VIP) shippingFee = 0;
        order.setShippingFee(shippingFee);

        int payableAmount = afterDiscount + shippingFee;
        order.setPayableAmount(payableAmount);

        switch (payMethod) {
            case POINT -> {
                if (member.getPoints() < payableAmount) {
                    throw new IllegalStateException("포인트가 부족합니다.");
                }
                member.setPoints(member.getPoints() - payableAmount);
                order.setStatus(OrderStatus.PREPARING);
            }
            case BANK_TRANSFER, TOSSPAY -> order.setStatus(OrderStatus.CREATED);
            default -> throw new IllegalArgumentException("지원하지 않는 결제수단: " + payMethod);
        }

        return order;
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return date + "-" + random;
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrders(Long memberMno) {
        return orderRepository.findByMember_MnoOrderByOrderDateDesc(memberMno)
                .stream()
                .map(OrderDetailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getRecentOrders(Long memberMno) {
        return orderRepository.findRecentOrders(memberMno, PageRequest.of(0, 5))
                .stream()
                .map(OrderDetailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long memberMno) {
        return orderRepository.findByIdWithMember(orderId, memberMno)
                .map(OrderDetailResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("주문 없음: " + orderId));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderDetailResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("주문 없음: " + orderId));
    }

    @Transactional
    public void confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문 없음"));

        if (order.getPayMethod() != Pay.BANK_TRANSFER) {
            throw new IllegalStateException("무통장 결제만 수동 확인 가능합니다.");
        }
        if (order.getAddress() == null) {
            throw new IllegalStateException("배송지가 설정되지 않았습니다.");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("결제 대기 상태가 아님");
        }

        order.setStatus(OrderStatus.PREPARING);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문 없음: " + orderId));
        order.setStatus(status);
    }

    @Transactional
    public Long createSelectedOrder(Long memberId, List<Long> cartItemIds, Long addressId, Pay payMethod) {
        Member member = getMember(memberId);
        Cart cart = getCart(memberId);

        List<CartItem> selectedItems = cartItemRepository.findByCartAndCartItemIdIn(cart, cartItemIds);
        if (selectedItems.isEmpty()) throw new IllegalStateException("선택된 장바구니 항목이 없습니다.");

        Address address = (addressId != null)
                ? addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지 없음: " + addressId))
                : addressRepository.findFirstByMember_MnoAndIsDefaultTrue(member.getMno())
                .orElseThrow(() -> new IllegalStateException("기본 배송지가 설정되어 있지 않습니다."));

        Order order = buildOrder(member, selectedItems, address, payMethod);
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAllByCartAndCartItemIdIn(cart, cartItemIds);
        cartItemRepository.flush();
        cart.getItems().removeIf(ci -> cartItemIds.contains(ci.getCartItemId()));

        return savedOrder.getOrderId();
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 없음: " + orderId));

        // 본인 주문 여부
        if (!Objects.equals(order.getMember().getMno(), memberId)) {
            throw new IllegalStateException("본인 주문만 취소 가능");
        }

        // 중복/불가 상태 차단
        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalStateException("이미 취소/환불된 주문입니다.");
        }
        if (!(order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PREPARING)) {
            throw new IllegalStateException("취소 불가 상태");
        }

        // 1) 포인트 환급 (POINT 결제만)
        if (order.getPayMethod() == Pay.POINT) {
            Member member = order.getMember();
            int refund = order.getPayableAmount();
            if (refund > 0) {
                member.setPoints(member.getPoints() + refund);
                log.info("포인트 환급 완료: memberMno={}, +{}p, after={}",
                        member.getMno(), refund, member.getPoints());
            }
        } else {
            // PG(BANK_TRANSFER/TOSSPAY) 실제 환불은 별도 결제 취소 프로세스에서 처리
            log.info("PG 결제 주문 취소(도메인 상태만 변경): orderId={}, pay={}", orderId, order.getPayMethod());
        }

        // 2) 재고 복원
        for (OrderItem oi : order.getOrderItems()) {
            oi.getItem().increaseStock(oi.getQuantity());
        }

        // 3) 상태 전이
        order.setStatus(OrderStatus.CANCELED);
    }

    @Transactional
    public void requestRefund(Long orderId, Long memberId, RefundReason reason, String reasonDetail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));

        if (!order.getMember().getMno().equals(memberId)) {
            throw new IllegalStateException("본인 주문만 환불 요청 가능");
        }
        if (!(order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED)) {
            throw new IllegalStateException("환불은 배송완료 또는 주문완료 상태에서만 가능합니다.");
        }

        OrderRefund refund = OrderRefund.builder()
                .order(order)
                .reasonCode(reason)
                .reasonDetail(reasonDetail)
                .build();
        orderRefundRepository.save(refund);

        order.setStatus(OrderStatus.REFUND_REQUESTED);
    }
}