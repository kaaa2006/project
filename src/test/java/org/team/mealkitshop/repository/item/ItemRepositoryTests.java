package org.team.mealkitshop.repository.item;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ItemRepositoryTests {

    @Autowired ItemRepository itemRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired MemberRepository memberRepository;

    @PersistenceContext   // 또는 @Autowired 가능
    private EntityManager em;

    Long itemId;
    Long memberId;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setEmail("item-repo@test.com");
        m.setPassword("pw");
        m.setMemberName("아이템리포");
        m.setPhone("010-1111-2222");
        memberId = memberRepository.save(m).getMno();

        Item item = Item.builder()
                .itemNm("샐러드A")
                .price(9000)
                .stockNumber(50)
                .itemDetail("신선한 샐러드")
                .itemSellStatus(ItemSellStatus.SELL)
                .build();
        item.setFoodItem(FoodItem.SALAD); // → category REFRIGERATED
        itemId = itemRepository.save(item).getId();

        Review r1 = new Review(); r1.setItem(item); r1.setMember(m); r1.setContent("좋아요"); r1.setRating(5);
        Review r2 = new Review(); r2.setItem(item); r2.setMember(m); r2.setContent("무난");   r2.setRating(4);

        item.addReview(r1);
        item.addReview(r2);

        reviewRepository.save(r1); reviewRepository.save(r2);

        em.flush();   // INSERT/UPDATE를 DB에 반영
        em.clear();   // 1차 캐시 비우기
    }

    @Test @DisplayName("findById 기본 동작 / foodItem↔category 동기화 확인")
    void findById_ok() {
        Optional<Item> found = itemRepository.findById(itemId);
        assertThat(found).isPresent();
        assertThat(found.get().getFoodItem()).isEqualTo(FoodItem.SALAD);
        assertThat(found.get().getCategory()).isEqualTo(Category.REFRIGERATED);
    }

    @Test @DisplayName("카테고리/판매상태 조건 페이지 조회")
    void findByCategoryAndStatus() {
        var page = itemRepository.findByCategoryAndItemSellStatus(
                Category.REFRIGERATED, ItemSellStatus.SELL, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).extracting("id").contains(itemId);
    }

    @Test @DisplayName("엔티티 그래프: 리뷰만 페치")
    void findWithReviewsById() {
        var opt = itemRepository.findWithReviewsById(itemId);
        assertThat(opt).isPresent();
        var item = opt.get();
        assertThat(item.getReviews()).hasSize(2);
    }

    @Test @DisplayName("엔티티 그래프: 리뷰 + 작성자까지 페치")
    void findItemWithAllReviewsAndAuthors() {
        var opt = itemRepository.findItemWithAllReviewsAndAuthors(itemId);
        assertThat(opt).isPresent();
        var item = opt.get();
        assertThat(item.getReviews()).hasSize(2);
        var name = item.getReviews().get(0).getMember().getMemberName();
        assertThat(name).isEqualTo("아이템리포");
    }
}
