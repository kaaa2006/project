package org.team.mealkitshop.service.cart;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.cart.AddToCartRequest;
import org.team.mealkitshop.dto.cart.UpdateCartItemRequest;
import org.team.mealkitshop.dto.cart.CartDetailResponse;
import org.team.mealkitshop.dto.cart.AmountSummary;


import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.service.cart.CartService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Commit
class CartServiceTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    CartService cartService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    CartItemRepository cartItemRepository;

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 15);
    }

    private Member newMember() {
        String unique = uniqueSuffix();
        Member member = Member.builder()
                .email("test" + unique + "@example.com")
                .memberName(("testUser-" + unique).substring(0, 15))
                .password("{noop}1234")
                .phone("010-" + (int)(Math.random()*9000+1000) + "-" + (int)(Math.random()*9000+1000))
                .grade(Grade.BASIC)
                .build();
        return memberRepository.save(member);
    }

    private Item newItem(String name, int price) {
        String unique = uniqueSuffix();
        Item item = Item.builder()
                .itemNm(name + "-" + unique)
                .price(price)
                .stockNumber(100)
                .itemDetail("간단 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .foodItem(FoodItem.PROTEIN_DRINK)
                .build();
        return itemRepository.save(item);
    }

    private AddToCartRequest cartRequest(Long itemId, int quantity) {
        return AddToCartRequest.builder()
                .itemId(itemId)
                .quantity(quantity)
                .build();
    }

    private UpdateCartItemRequest updateRequest(Long cartItemId, Integer qty, Boolean checked) {
        return UpdateCartItemRequest.builder()
                .cartItemId(cartItemId)
                .quantity(qty)
                .checked(checked)
                .build();
    }

    @Test
    void 상품_담기_그리고_조회() {
        Member member = newMember();
        Item item = newItem("닭가슴살", 5000);

        Long cartItemId = cartService.addToCart(member.getMno(), cartRequest(item.getId(), 2));

        Cart cart = cartRepository.findByMember_Mno(member.getMno()).orElseThrow();
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow();

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cartItem.getQuantity()).isEqualTo(2);
        assertThat(cartItem.getItem().getItemNm()).contains("닭가슴살");
    }

    @Test
    void 같은_상품_두번_담으면_수량_합산() {
        Member member = newMember();
        Item item = newItem("프로틴", 7000);

        cartService.addToCart(member.getMno(), cartRequest(item.getId(), 1));
        cartService.addToCart(member.getMno(), cartRequest(item.getId(), 2));

        Cart cart = cartRepository.findByMember_Mno(member.getMno()).orElseThrow();
        CartItem cartItem = cart.getItems().get(0);

        assertThat(cartItem.getQuantity()).isEqualTo(3);
        assertThat(cart.getProductsTotal()).isEqualTo(3 * 7000);
    }

    @Test
    void 수량_변경과_삭제() {
        Member member = newMember();
        Item item = newItem("드레싱", 3000);

        Long lineId = cartService.addToCart(member.getMno(), cartRequest(item.getId(), 2));

        cartService.updateCartItem(member.getMno(), updateRequest(lineId, 0, null));
        cartItemRepository.flush();
        em.clear();

        assertThat(cartItemRepository.findById(lineId)).isEmpty();
    }

    @Test
    void 체크박스_상태_변경() {
        Member member = newMember();
        Item item = newItem("포케", 6000);

        Long lineId = cartService.addToCart(member.getMno(), cartRequest(item.getId(), 1));
        cartService.updateCartItem(member.getMno(), updateRequest(lineId, null, false));

        CartItem cartItem = cartItemRepository.findById(lineId).orElseThrow();
        assertThat(cartItem.isChecked()).isFalse();
    }

    @Test
    void 장바구니가_없으면_빈_결과() {
        Member member = newMember();

        CartDetailResponse response = cartService.getCartDetail(member.getMno(), "06236");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSummary().getProductsTotal()).isZero();
        assertThat(response.getSummary().getPayableAmount()).isZero();
    }

    @Test
    void 배송비_계산_기본과_제주() {
        Member member = newMember();
        Item item = newItem("식단", 10000);

        cartService.addToCart(member.getMno(), cartRequest(item.getId(), 2));

        CartDetailResponse seoul = cartService.getCartDetail(member.getMno(), "06236");
        CartDetailResponse jeju = cartService.getCartDetail(member.getMno(), "63000");

        assertThat(seoul.getSummary().getShippingFee()).isEqualTo(3000);
        assertThat(jeju.getSummary().getShippingFee()).isEqualTo(8000);
    }

    @Test
    void VIP회원은_쿠폰_10퍼센트_할인된다() {
        String unique = uniqueSuffix();
        Member vip = Member.builder()
                .email("vip" + unique + "@test.com")
                .memberName(("VIP회원-" + unique).substring(0, 15))
                .password("{noop}1234")
                .phone("010-" + (int)(Math.random()*9000+1000) + "-" + (int)(Math.random()*9000+1000))
                .grade(Grade.VIP)
                .build();
        memberRepository.save(vip);

        Item item1 = newItem("3주일치 식단", 40000);
        Item item2 = newItem("1주일치 식단", 15000);

        cartService.addToCart(vip.getMno(), cartRequest(item1.getId(), 1));
        cartService.addToCart(vip.getMno(), cartRequest(item2.getId(), 1));

        CartDetailResponse response = cartService.getCartDetail(vip.getMno(), "06236");

        AmountSummary summary = response.getSummary();
        assertThat(summary.getProductsTotal()).isEqualTo(55000);
        assertThat(summary.getShippingFee()).isZero();
        int expectedDiscount = (int) Math.round(55000 * 0.10);
        assertThat(summary.getCouponDiscount()).isEqualTo(expectedDiscount);
        assertThat(summary.getAppliedCouponCode()).isEqualTo("VIP");
        int expectedPayable = 55000 - expectedDiscount;
        assertThat(summary.getPayableAmount()).isEqualTo(expectedPayable);
    }

    @Test
    void 상품에_판매상태와_분류가_저장된다() {
        Member member = newMember();
        Item item = newItem("프로틴워터", 8000);

        cartService.addToCart(member.getMno(), cartRequest(item.getId(), 1));
        Cart cart = cartRepository.findByMember_Mno(member.getMno()).orElseThrow();
        CartItem cartItem = cart.getItems().get(0);

        assertThat(cartItem.getItem().getItemSellStatus()).isEqualTo(ItemSellStatus.SELL);
        assertThat(cartItem.getItem().getFoodItem()).isEqualTo(FoodItem.PROTEIN_DRINK);
        assertThat(cartItem.getItem().getFoodItem().getCategory().getLabel()).isEqualTo("기타");
    }

    /**
     * 여러 종류의 상품을 미리 장바구니에 담아두기 위한 테스트
     * 프론트에서 조회 테스트용 (console에 mno 출력됨)
     */
    @Test
    void 여러_종류_상품을_미리_담아둔다() {
        Member member = newMember();

        Item chicken  = newItem("닭가슴살",    5000);
        Item salad    = newItem("샐러드",      8000);
        Item poke     = newItem("포케볼",      9000);
        Item drink    = newItem("단백질음료",   6000);
        Item dressing = newItem("드레싱",      2500);
        Item fried    = newItem("볶음밥",      6500);
        Item setMeal  = newItem("다이어트세트",12000);

        cartService.addToCart(member.getMno(), cartRequest(chicken.getId(), 2));
        cartService.addToCart(member.getMno(), cartRequest(salad.getId(), 1));
        cartService.addToCart(member.getMno(), cartRequest(poke.getId(), 3));
        cartService.addToCart(member.getMno(), cartRequest(drink.getId(), 4));
        cartService.addToCart(member.getMno(), cartRequest(dressing.getId(),1));
        cartService.addToCart(member.getMno(), cartRequest(fried.getId(), 2));
        cartService.addToCart(member.getMno(), cartRequest(setMeal.getId(),1));

        CartDetailResponse res = cartService.getCartDetail(member.getMno(), "06236");
        assertThat(res.getItems()).hasSize(7);

        System.out.println("========== 프론트에서 확인할 memberMno = " + member.getMno() + " ==========");
    }
}
