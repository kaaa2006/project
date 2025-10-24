package org.team.mealkitshop.repository.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Member;

import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // ✅ 임베디드 교체 금지
@Transactional
public class CartItemRepositoryTest {

    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    MemberRepository memberRepository;

    private Member createMember() {
        String uniq = UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.builder()
                .email("qghujr@naver.com")
                .password("pw")
                .memberName("qwer")
                .phone("01011234441")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .provider(Provider.Local)
                .grade(Grade.BASIC)
                .marketingYn(false)
                .points(0)
                .build());
    }

    private Item createItem(String name) {
        return itemRepository.save(Item.builder()
                .itemNm(name)
                .itemDetail("상세")
                .price(1000)
                .stockNumber(10)
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.ETC)
                .foodItem(FoodItem.DRESSING)
                .build());
    }

    private Cart createCart(Member member) {
        return cartRepository.save(Cart.createFor(member));
    }

    private CartItem createCartItem(Cart cart, Item item, int qty, boolean checked) {
        return cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .item(item)
                .quantity(qty)
                .checked(checked)
                .build());
    }

    @Test
    @DisplayName("findByCartAndItem - 이미 담긴 상품 찾기")
    void findByCartAndItem_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        Item item = createItem("닭가슴살");
        createCartItem(cart, item, 3, true);

        Optional<CartItem> found = cartItemRepository.findByCartAndItem(cart, item);
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAllByCart - 장바구니 전체 조회")
    void findAllByCart_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        createCartItem(cart, createItem("식단1"), 2, false);
        createCartItem(cart, createItem("식단2"), 1, true);

        List<CartItem> all = cartItemRepository.findAllByCart(cart);
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("findAllByCartAndCartItemIdIn - 소유 항목 필터링")
    void findAllByCartAndCartItemIdIn_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        CartItem c1 = createCartItem(cart, createItem("식단1"), 2, false);
        CartItem c2 = createCartItem(cart, createItem("식단2"), 1, true);

        List<CartItem> list = cartItemRepository.findAllByCartAndCartItemIdIn(
                cart, List.of(c1.getCartItemId()));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCartItemId()).isEqualTo(c1.getCartItemId());
    }

    @Test
    @DisplayName("findByCartAndCartItemId - 소유 단일 항목 조회")
    void findByCartAndCartItemId_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        CartItem c1 = createCartItem(cart, createItem("식단1"), 2, false);

        Optional<CartItem> found = cartItemRepository.findByCartAndCartItemId(cart, c1.getCartItemId());

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllByCartAndCheckedTrue - 체크된 항목만 조회")
    void findAllByCartAndCheckedTrue_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        createCartItem(cart, createItem("선택상품1"), 1, true);
        createCartItem(cart, createItem("미선택상품"), 1, false);

        List<CartItem> checked = cartItemRepository.findAllByCartAndCheckedTrue(cart);
        assertThat(checked).hasSize(1);
        assertThat(checked.get(0).isChecked()).isTrue();
    }

    @Test
    @DisplayName("deleteAllByCartAndCartItemIdIn - 일괄 삭제")
    void deleteAllByCartAndCartItemIdIn_test() {
        Member member = createMember();
        Cart cart = createCart(member);
        CartItem c1 = createCartItem(cart, createItem("삭제대상1"), 1, true);
        CartItem c2 = createCartItem(cart, createItem("삭제대상2"), 2, false);

        int deletedCount = cartItemRepository.deleteAllByCartAndCartItemIdIn(cart, List.of(
                c1.getCartItemId(), c2.getCartItemId()));

        assertThat(deletedCount).isEqualTo(2);

        List<CartItem> after = cartItemRepository.findAllByCart(cart);
        assertThat(after).isEmpty();
    }
}
