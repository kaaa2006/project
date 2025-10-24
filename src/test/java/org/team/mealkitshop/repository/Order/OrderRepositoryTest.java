package org.team.mealkitshop.repository.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.config.RootConfig;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderItem;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(RootConfig.class)
class OrderRepositoryTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ItemRepository itemRepository;

    @DisplayName("addressId로 배송지 조회 후 주문 저장 테스트")
    @Test
    @Commit // 테스트 후 롤백하지 않고 DB에 커밋
    void saveOrder_withAddressId() {
        // 1) 회원 저장
        Member member = Member.builder()
                .email("user" + System.currentTimeMillis() + "@gmail.com")
                .memberName("회원" + System.nanoTime())
                .phone("010-" + (int)(Math.random()*9000+1000) + "-" + (int)(Math.random()*9000+1000))
                .password("123456")
                .grade(Grade.VIP)
                .points(0)
                .marketingYn(false)
                .role(Role.ADMIN)
                .provider(Provider.Local)
                .status(Status.ACTIVE)
                .build();
        memberRepository.save(member);

        // 2) 배송지 저장
        Address savedAddress = addressRepository.save(Address.builder()
                .member(member)
                .zipCode("12345")
                .addr1("서울시 강남구")
                .addr2("테스트동 101호")
                .alias("집")
                .isDefault(true)
                .build());

        Long addressId = savedAddress.getAddressId();

        // 3) 상품 저장
        Item savedItem = itemRepository.save(Item.builder()
                .itemNm("테스트상품")
                .price(10000)
                .stockNumber(50)
                .itemDetail("테스트 상품 상세 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.FROZEN)
                .foodItem(FoodItem.SALAD)
                .build());

        // 4) 주문 생성
        Address foundAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지 없음"));

        Order order = Order.builder()
                .orderNo("TEST-" + System.currentTimeMillis()) // ← 유니크 보장
                .member(member)
                .address(foundAddress)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.CREATED)
                .productsTotal(10000)
                .discountTotal(0)
                .shippingFee(0)
                .payableAmount(10000)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .build();


        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .item(savedItem)
                .quantity(2)
                .build();

        order.addItem(orderItem);

        // 5) 저장
        Order savedOrder = orderRepository.save(order);

        // 6) 검증
        assertThat(savedOrder.getOrderId()).isNotNull();
        assertThat(savedOrder.getAddress().getAddressId()).isEqualTo(addressId);
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().get(0).getItem().getItemNm())
                .isEqualTo("테스트상품"); // PK가 아니라 이름 비교로 수정
    }
}
