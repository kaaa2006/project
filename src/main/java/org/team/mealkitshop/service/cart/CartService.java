// [파일 상단 import 및 선언부는 동일 — 생략하지 않음]
package org.team.mealkitshop.service.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.cart.*;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final ItemImgRepository itemImgRepository;

    private Cart getOrCreateCartByMemberMno(Long mno) {
        return cartRepository.findByMember_Mno(mno)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(mno)
                            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + mno));
                    return cartRepository.save(Cart.createFor(member));
                });
    }

    private CartItem getOwnedCartItem(Long memberMno, Long cartItemId) {
        Cart cart = cartRepository.findByMember_Mno(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 장바구니가 없습니다. memberId=" + memberMno));
        return cartItemRepository.findByCartAndCartItemId(cart, cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장바구니 항목이 없습니다. cartItemId=" + cartItemId));
    }

    private int getGradeCouponPercent(Grade grade) {
        return switch (grade) {
            case VIP -> 10;
            case GOLD -> 7;
            case SILVER -> 5;
            default -> 0;
        };
    }

    private CartItemDto toDto(CartItem ci, String thumbnailUrl) {
        return CartItemDto.builder()
                .cartItemId(ci.getCartItemId())
                .itemId(ci.getItem().getId())
                .itemName(ci.getItem().getItemNm())
                .originalPrice(ci.getItem().getOriginalPrice())
                .discountRate(ci.getItem().getDiscountRate())
                .salePrice(ci.getItem().getSalePrice())
                .quantity(ci.getQuantity())
                .checked(ci.isChecked())
                .lineTotal(ci.getLineTotal())
                .lineDiscountTotal(ci.getLineDiscountTotal())
                .linePayable(ci.getLinePayable())
                .stock(ci.getItem().getStockNumber())   // ★ 추가 (재고 필드 매핑)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private AmountSummary toAllSummaryWithCoupon(Cart cart, Member member, String zipcode) {
        int subtotal = cart.getSalePriceTotal();
        int shippingFee = cart.getEstimatedShippingFee(zipcode);

        Grade grade = member != null ? member.getGrade() : null;
        if (Grade.VIP.equals(grade)) {
            shippingFee = 0;
        }

        int percent = getGradeCouponPercent(grade);
        int couponDiscount = Math.min((int) Math.round(subtotal * (percent / 100.0)), subtotal);
        int payableAmount = Math.max(0, subtotal + shippingFee - couponDiscount);

        return AmountSummary.builder()
                .productsTotal(subtotal)
                .shippingFee(shippingFee)
                .payableAmount(payableAmount)
                .couponDiscount(couponDiscount)
                .appliedCouponCode(grade != null ? grade.name() : null)
                .build();
    }

    // ✅ 수정된 부분
    private AmountSummary toCheckedSummaryWithCoupon(Cart cart, Member member, String zipcode) {
        int subtotal = cart.getCheckedSalePriceTotal();
        int shippingFee = 0;

        if (subtotal < 50_000 && subtotal > 0) {
            shippingFee += 3_000;
        }

        if (zipcode != null && zipcode.startsWith("63")) {
            shippingFee += 5_000;
        }

        Grade grade = member != null ? member.getGrade() : null;
        if (Grade.VIP.equals(grade)) {
            shippingFee = 0;
        }

        int percent = getGradeCouponPercent(grade);
        int couponDiscount = Math.min((int) Math.round(subtotal * (percent / 100.0)), subtotal);
        int payableAmount = Math.max(0, subtotal + shippingFee - couponDiscount);

        return AmountSummary.builder()
                .productsTotal(subtotal)
                .shippingFee(shippingFee)
                .payableAmount(payableAmount)
                .couponDiscount(couponDiscount)
                .appliedCouponCode(grade != null ? grade.name() : null)
                .build();
    }

    public Long addToCart(Long memberMno, AddToCartRequest req) {
        Cart cart = getOrCreateCartByMemberMno(memberMno);
        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. id=" + req.getItemId()));

        Optional<CartItem> found = cartItemRepository.findByCartAndItem(cart, item);
        if (found.isPresent()) {
            CartItem existing = found.get();
            existing.increaseQuantity(Math.max(1, req.getQuantity()));
            return existing.getCartItemId();
        }

        CartItem newItem = CartItem.builder()
                .item(item)
                .quantity(Math.max(1, req.getQuantity()))
                .checked(true)
                .build();

        cart.addItem(newItem);
        return cartItemRepository.save(newItem).getCartItemId();
    }

    public void updateCartItem(Long memberMno, UpdateCartItemRequest req) {
        CartItem line = getOwnedCartItem(memberMno, req.getCartItemId());

        if (req.getQuantity() != null) {
            int qty = req.getQuantity();
            if (qty <= 0) {
                Cart owner = line.getCart();
                cartItemRepository.delete(line);
                cartItemRepository.flush();
                owner.getItems().removeIf(ci -> ci.getCartItemId().equals(line.getCartItemId()));
                return;
            } else {
                line.changeQuantity(qty);
            }
        }

        if (req.getChecked() != null) {
            boolean target = req.getChecked();
            if (line.isChecked() != target) {
                line.toggleChecked();
            }
        }
    }

    @Transactional(readOnly = true)
    public CartDetailResponse getCartDetail(Long memberMno, String zipcode) {
        Cart cart = cartRepository.findByMember_Mno(memberMno).orElse(null);

        if (cart == null) {
            Member member = memberRepository.findById(memberMno).orElse(null);
            String grade = (member != null && member.getGrade() != null) ? member.getGrade().name() : null;

            AmountSummary emptySummary = AmountSummary.builder()
                    .productsTotal(0)
                    .shippingFee(0)
                    .payableAmount(0)
                    .couponDiscount(0)
                    .appliedCouponCode(grade)
                    .build();

            return CartDetailResponse.builder()
                    .cartId(null)
                    .memberId(memberMno)
                    .items(List.of())
                    .summary(emptySummary)
                    .checkedSummary(emptySummary)
                    .build();
        }

        List<Long> itemIds = cart.getItems().stream()
                .map(ci -> ci.getItem().getId())
                .distinct()
                .toList();

        Map<Long, String> repUrlMap = itemImgRepository.findRepUrlsByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        ItemImgRepository.ItemRepProjection::getItemId,
                        ItemImgRepository.ItemRepProjection::getImgUrl
                ));

        List<CartItemDto> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            String thumbnailUrl = repUrlMap.get(ci.getItem().getId());
            items.add(toDto(ci, thumbnailUrl));
        }

        Member member = cart.getMember();
        AmountSummary summary = toAllSummaryWithCoupon(cart, member, zipcode);
        AmountSummary checkedSummary = toCheckedSummaryWithCoupon(cart, member, zipcode);

        return CartDetailResponse.builder()
                .cartId(cart.getCartId())
                .memberId(memberMno)
                .items(items)
                .summary(summary)
                .checkedSummary(checkedSummary)
                .build();
    }

    @Transactional(readOnly = true)
    public CartDetailResponse getSelectedCartDetail(Long memberMno, List<Long> cartItemIds) {
        return getSelectedCartDetail(memberMno, cartItemIds, null);
    }

    // ✅ 수정된 부분 (VIP 무료배송 반영)
    @Transactional(readOnly = true)
    public CartDetailResponse getSelectedCartDetail(Long memberMno, List<Long> cartItemIds, String zipcode) {
        Cart cart = cartRepository.findByMember_Mno(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 없음: " + memberMno));

        List<CartItem> selectedItems = cartItemRepository.findByCartAndCartItemIdIn(cart, cartItemIds);

        List<Long> itemIds = selectedItems.stream()
                .map(ci -> ci.getItem().getId())
                .distinct()
                .toList();

        Map<Long, String> repUrlMap = itemImgRepository.findRepUrlsByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        ItemImgRepository.ItemRepProjection::getItemId,
                        ItemImgRepository.ItemRepProjection::getImgUrl
                ));

        List<CartItemDto> items = new ArrayList<>();
        for (CartItem ci : selectedItems) {
            String thumbnailUrl = repUrlMap.get(ci.getItem().getId());
            items.add(toDto(ci, thumbnailUrl));
        }

        Member member = cart.getMember();
        int subtotal = items.stream().mapToInt(CartItemDto::getLinePayable).sum();

        int shippingFee = 0;
        if (subtotal < 50_000 && subtotal > 0) {
            shippingFee += 3_000;
        }
        if (zipcode != null && zipcode.startsWith("63")) {
            shippingFee += 5_000;
        }

        // ✅ 추가: VIP 등급은 무료배송 (장바구니 화면 규칙과 동일)
        Grade grade = (member != null) ? member.getGrade() : null;
        if (Grade.VIP.equals(grade)) {
            shippingFee = 0;
        }

        int couponDiscount = 0;
        if (member != null) {
            int percent = getGradeCouponPercent(member.getGrade());
            couponDiscount = Math.min((int) Math.round(subtotal * (percent / 100.0)), subtotal);
        }

        int payableAmount = Math.max(0, subtotal + shippingFee - couponDiscount);

        AmountSummary summary = AmountSummary.builder()
                .productsTotal(subtotal)
                .shippingFee(shippingFee)
                .payableAmount(payableAmount)
                .couponDiscount(couponDiscount)
                .appliedCouponCode(member != null && member.getGrade() != null ? member.getGrade().name() : null)
                .build();

        return CartDetailResponse.builder()
                .cartId(cart.getCartId())
                .memberId(memberMno)
                .items(items)
                .summary(summary)
                .checkedSummary(summary)
                .build();
    }

    public void clearCart(Long memberMno) {
        Cart cart = cartRepository.findByMember_Mno(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 없음. id=" + memberMno));
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
    }

    public int getCartItemCount(Long memberId) {
        return cartItemRepository.countByCart_Member_Mno(memberId);
    }

    public void deleteCheckedItems(Long memberId) {
        Cart cart = cartRepository.findByMember_Mno(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 없음"));

        List<CartItem> toDelete = cartItemRepository.findAllByCartAndCheckedTrue(cart);
        cartItemRepository.deleteAll(toDelete);
        cart.getItems().removeIf(CartItem::isChecked);
    }

    public List<CartItemDto> getCheckedCartItems(Long memberId) {
        Cart cart = cartRepository.findByMember_Mno(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 없음"));

        List<CartItem> checkedItems = cartItemRepository.findAllByCartAndCheckedTrue(cart);

        List<Long> itemIds = checkedItems.stream()
                .map(ci -> ci.getItem().getId())
                .distinct()
                .toList();

        Map<Long, String> repUrlMap = itemImgRepository.findRepUrlsByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        ItemImgRepository.ItemRepProjection::getItemId,
                        ItemImgRepository.ItemRepProjection::getImgUrl
                ));

        return checkedItems.stream()
                .map(ci -> toDto(ci, repUrlMap.get(ci.getItem().getId())))
                .toList();
    }
}
