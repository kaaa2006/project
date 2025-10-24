package org.team.mealkitshop.repository.order;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.config.RootConfig;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(RootConfig.class)
class OrderItemRepositoryTest {



    @PersistenceContext
    private EntityManager em;

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ItemRepository itemRepository;


    @Test
    @DisplayName("OrderItemRepository 메서드 전체 동작 테스트")
    void testOrderItemRepositoryMethods() {
        // ===== 1. 회원 생성 =====
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

        // ===== 2. 배송지 생성 =====
        Address address = addressRepository.save(Address.builder()
                .member(member)
                .zipCode("12345")
                .addr1("서울시 강남구")
                .addr2("테스트동 101호")
                .alias("집")
                .isDefault(true)
                .build());

        // ===== 3. 상품 생성 =====
        Item item1 = itemRepository.save(Item.builder()
                .itemNm("상품1")
                .price(5000)
                .stockNumber(100)
                .itemDetail("상품1 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.FROZEN)
                .foodItem(FoodItem.SALAD)
                .build());

        Item item2 = itemRepository.save(Item.builder()
                .itemNm("상품2")
                .price(10000)
                .stockNumber(50)
                .itemDetail("상품2 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .category(Category.FROZEN)
                .foodItem(FoodItem.SALAD)
                .build());

        // ===== 4. 주문 생성 =====
        Order order = Order.builder()
                .orderNo("20250818-ORDERITEM")
                .member(member)
                .address(address)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.CREATED)
                .productsTotal(15000)
                .discountTotal(0)
                .shippingFee(0)
                .payableAmount(15000)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .build();

        OrderItem orderItem1 = OrderItem.builder()
                .order(order)
                .item(item1)
                .quantity(2)
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .order(order)
                .item(item2)
                .quantity(1)
                .build();

        order.addItem(orderItem1);
        order.addItem(orderItem2);

        orderRepository.save(order);

// 🔥 flush 추가 (저장 SQL을 DB에 강제로 날림)
        orderRepository.flush();

        Long orderId = order.getOrderId();

// ===== 5. findByOrder_OrderId =====
        List<OrderItem> itemsByOrder = orderItemRepository.findByOrder_OrderId(orderId);
        assertThat(itemsByOrder).hasSize(2);

// ===== 6. existsByOrder_OrderIdAndItem_Id =====
        boolean exists = orderItemRepository.existsByOrder_OrderIdAndItem_Id(orderId, item1.getId());
        assertThat(exists).isTrue();

// ===== 7. findByOrder_OrderIdIn =====
        List<OrderItem> batchItems = orderItemRepository.findByOrder_OrderIdIn(List.of(orderId));
        assertThat(batchItems).hasSize(2);

// ===== 8. findWithItemByOrderId (fetch join) =====
        List<OrderItem> withItem = orderItemRepository.findWithItemByOrderId(orderId);
        assertThat(withItem.get(0).getItem().getItemNm()).isNotNull();

// ===== 9. deleteByOrder_OrderId =====
        orderItemRepository.deleteByOrder_OrderId(orderId);
        orderItemRepository.flush(); // DB에 즉시 반영
        em.clear(); // (EntityManager 주입 필요) 1차 캐시 초기화

        List<OrderItem> afterDelete = orderItemRepository.findByOrder_OrderId(orderId);
        assertThat(afterDelete).isEmpty();



    }
}
