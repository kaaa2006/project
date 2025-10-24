package org.team.mealkitshop.service.Cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.config.RootConfig;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.dto.checkout.CreateOrderRequest;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.service.order.OrderService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(RootConfig.class)
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ItemRepository itemRepository;

    @Test
    @DisplayName("장바구니 → 주문 생성 전체 플로우 테스트")
    void createOrder_success() {
        // === 1. 회원 생성 ===
        Member member = memberRepository.save(Member.builder()
                .email("user" + System.currentTimeMillis() + "@gmail.com")
                .memberName("테스트회원" + System.currentTimeMillis()) // 이름도 랜덤화
                .phone("010-" + (int)(Math.random()*9000+1000) + "-" + (int)(Math.random()*9000+1000))
                .password("123456")
                .grade(Grade.VIP) // VIP라서 할인 적용
                .points(0)
                .marketingYn(false)
                .role(Role.USER)
                .provider(Provider.Local)
                .status(Status.ACTIVE)
                .build());

        // === 2. 배송지 생성 ===
        Address address = addressRepository.save(Address.builder()
                .member(member)
                .zipCode("12345")
                .addr1("서울시 강남구")
                .addr2("테스트 아파트 101동")
                .alias("집")
                .isDefault(true)
                .build());

        // === 3. 장바구니 & 아이템 생성 ===
        Cart cart = cartRepository.save(Cart.builder()
                .member(member)
                .build());

        Item item1 = itemRepository.save(Item.builder()
                .itemNm("샐러드")
                .originalPrice(10000)
                .stockNumber(100)
                .itemDetail("신선한 샐러드")
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.ETC)
                .foodItem(FoodItem.SALAD)
                .build());

        Item item2 = itemRepository.save(Item.builder()
                .itemNm("치킨")
                .originalPrice(20000)
                .stockNumber(50)
                .itemDetail("바삭한 치킨")
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.FROZEN)
                .foodItem(FoodItem.CHICKEN_BREAST)
                .build());

        CartItem cartItem1 = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .item(item1)
                .quantity(2)
                .build());

        CartItem cartItem2 = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .item(item2)
                .quantity(1)
                .build());

        // === 4. 주문 생성 요청 DTO ===
        CreateOrderRequest req = CreateOrderRequest.builder()
                .memberMno(member.getMno())
                .addressId(address.getAddressId())
                .receiverName("홍길동")
                .receiverPhone("010-2222-3333")
                .cartItemIds(List.of(cartItem1.getCartItemId(), cartItem2.getCartItemId()))
                .build();

        // === 5. 서비스 호출 ===
        Order order = orderService.createOrder();

        // === 6. 검증 ===
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getOrderItems()).hasSize(2); // 장바구니 2개 → 주문 2개 라인
        assertThat(order.getProductsTotal()).isEqualTo(10000 * 2 + 20000 * 1); // 40,000
        assertThat(order.getDiscountTotal()).isGreaterThanOrEqualTo(0); // VIP라 할인
        assertThat(order.getPayableAmount()).isGreaterThan(0);

        // 장바구니 비워졌는지도 체크
        List<CartItem> remainingCartItems = cartItemRepository.findAllByCart(cart);
        assertThat(remainingCartItems).isEmpty();
    }
}
