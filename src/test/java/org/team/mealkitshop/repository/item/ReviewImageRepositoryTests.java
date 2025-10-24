package org.team.mealkitshop.repository.item;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewImage;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ReviewImageRepositoryTests {

    @Autowired ItemRepository itemRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReviewImageRepository reviewImageRepository;
    @Autowired MemberRepository memberRepository;

    Long reviewId;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setEmail("rvimg-repo@test.com"); m.setPassword("pw");
        m.setMemberName("리뷰이미지"); m.setPhone("010-5555-6666");
        memberRepository.save(m);

        Item item = Item.builder()
                .itemNm("포케A").price(13000).stockNumber(30)
                .itemDetail("포케").itemSellStatus(ItemSellStatus.SELL).build();
        item.setFoodItem(FoodItem.POKE);
        itemRepository.save(item);

        Review r = new Review();
        r.setItem(item); r.setMember(m); r.setContent("사진첨부"); r.setRating(5);
        reviewId = reviewRepository.save(r).getId();

        ReviewImage img = new ReviewImage();
        img.setReview(r);
        img.setImgName("re-1.jpg");
        img.setOriImgName("ori-re-1.jpg");
        img.setImgUrl("/images/re-1.jpg");
        reviewImageRepository.save(img);
    }

    @Test @DisplayName("리뷰별 이미지 조회")
    void findByReviewId() {
        var list = reviewImageRepository.findByReview_IdOrderByIdAsc(reviewId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getImgName()).isEqualTo("re-1.jpg");
    }
}
