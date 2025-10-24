package org.team.mealkitshop.repository.item;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewImage;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewRepositoryTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig{
        @Bean
        AuditorAware<String> auditorAware(){
            return () -> Optional.of("test-auditor");
        }
    }

    @Autowired
    EntityManager em;

    @Autowired ReviewRepository reviewRepository;
    @Autowired ReviewImageRepository reviewImageRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired MemberRepository memberRepository;

    private Member makeMember(String email, String name) {
        Member m = new Member();
        m.setEmail(email);
        m.setMemberName(name);
        m.setPassword("$2a$10$abcdefghijklmnopqrstuvwx.yzABCDEFGHijklmnopqrs");
        m.setRole(org.team.mealkitshop.common.Role.USER);
        m.setProvider(org.team.mealkitshop.common.Provider.Local);
        m.setStatus(org.team.mealkitshop.common.Status.ACTIVE);
        m.setGrade(org.team.mealkitshop.common.Grade.BASIC);
        m.setPoints(0);
        m.setPhone("010-0000-0000");

        try {
            var f = m.getClass().getDeclaredField("marketingYn");
            f.setAccessible(true);
            f.set(m, Boolean.FALSE);
        } catch (NoSuchFieldException ignore) {
            // 엔티티에 없으면 패스
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return memberRepository.save(m);

    }

    private Item makeItem(String name, int price) {
        Item it = new Item();
        it.setItemNm(name);
        it.setItemDetail("detail-" + name);
        it.setOriginalPrice(price);
        it.setItemSellStatus(ItemSellStatus.SELL);
        it.setFoodItem(FoodItem.SET);
        return itemRepository.save(it);
    }

    private Review makeReview(Member writer, Item item, String content, int rating) {
        Review r = new Review();
        r.setMember(writer);
        r.setItem(item);
        r.changeContent(content);
        r.changeRating(rating);
        return reviewRepository.save(r);
    }

    private ReviewImage makeReviewImage(Review r, String fullPath) {
        // fullPath 예: "/images/r1-1.jpg"
        String fileName;
        String dir;

        int idx = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        if (idx >= 0) {
            dir = fullPath.substring(0, idx);           // "/images"
            fileName = fullPath.substring(idx + 1);     // "r1-1.jpg"
        } else {
            dir = "";                                   // 경로가 없을 수도 있음
            fileName = fullPath;
        }

        // 엔티티 실제 필드명에 맞춰서 세팅하세요.
        ReviewImage img = ReviewImage.builder()
                .review(r)          // 소유측 세팅(중요)
                .imgUrl(dir)        // 디렉토리/URL
                .imgName(fileName)  //NOT NULL
                .oriImgName(fileName) //NOT NULL (필요 시)
                // .repImgYn("N")   // 대표이미지 여부 같은 필드가 있으면 기본값
                .build();

        // Review에 편의 메서드가 있으면 동기화
        // r.addImage(img);

        return reviewImageRepository.save(img);
    }

    @Test
    @DisplayName("findWithImagesByItemId: 리뷰 + 이미지까지 페치 - 페이징")
    void findWithImagesByItemId() {
        Member m = makeMember("u1@test.com", "u1");
        Item it = makeItem("A", 1000);       // ⚠️ makeItem 안에서 stockNumber, foodItem 세팅 필수
        Review r = makeReview(m, it, "good", 5);
        makeReviewImage(r, "/images/r1-1.jpg");
        makeReviewImage(r, "/images/r1-2.jpg");

        em.flush();
        em.clear();                           //  1차 캐시 초기화

        Page<Review> page = reviewRepository.findWithImagesByItemId(it.getId(), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        Review got = page.getContent().get(0);
        assertEquals("good", got.getContent());
        assertEquals(2, got.getImages().size());
    }

    @Test
    @DisplayName("existsByMember_MnoAndItem_Id: 회원×상품 1회 작성 제약")
    void existsByMemberAndItem() {
        Member m = makeMember("u2@test.com", "u2");
        Item it = makeItem("B", 2000);
        makeReview(m, it, "ok", 4);

        boolean exists = reviewRepository.existsByMember_MnoAndItem_Id(m.getMno(), it.getId());
        assertTrue(exists);
    }

    @Test
    @DisplayName("getAverageRatingByItemId / countByItem_Id / Top10 정렬")
    void avgCountTop10() {
        Member m1 = makeMember("a@test.com", "a");
        Member m2 = makeMember("b@test.com", "b");
        Item it = makeItem("C", 3000);
        makeReview(m1, it, "r1", 5);
        makeReview(m2, it, "r2", 3);

        Double avg = reviewRepository.getAverageRatingByItemId(it.getId());
        assertNotNull(avg);
        assertEquals(4.0, avg, 0.0001);

        long cnt = reviewRepository.countByItem_Id(it.getId());
        assertEquals(2L, cnt);

        List<Review> top = reviewRepository.findTop10ByItem_IdOrderByRatingDescIdDesc(it.getId());
        assertFalse(top.isEmpty());
        assertTrue(top.get(0).getRating() >= top.get(top.size()-1).getRating());
    }

    @Test
    @DisplayName("findByItem_Id / findByMember_Mno: 페이징 조회")
    void pagingByItemAndMember() {
        Member m = makeMember("p@test.com", "p");
        Item it = makeItem("D", 4000);
        makeReview(m, it, "r1", 4);

        Page<Review> byItem = reviewRepository.findByItem_Id(it.getId(), PageRequest.of(0, 10));
        assertEquals(1, byItem.getTotalElements());

        Page<Review> byMember = reviewRepository.findByMember_Mno(m.getMno(), PageRequest.of(0, 10));
        assertEquals(1, byMember.getTotalElements());
    }

    @Test
    @DisplayName("findIdsByItemId / findWithItemAndMemberById")
    void idsAndSingleFetch() {
        Member m = makeMember("q@test.com", "q");
        Item it = makeItem("E", 5000);
        Review r = makeReview(m, it, "rX", 2);

        List<Long> ids = reviewRepository.findIdsByItemId(it.getId());
        assertEquals(1, ids.size());
        assertEquals(r.getId(), ids.get(0));

        Review one = reviewRepository.findWithItemAndMemberById(r.getId()).orElseThrow();
        assertNotNull(one.getMember());
        assertNotNull(one.getItem());
    }

    @Test
    @DisplayName("findReviewCountByItemIds: 프로젝션 반환 검사")
    void projectionCountByItemIds() {
        Member m = makeMember("z@test.com", "z");
        Member m2 = makeMember("y@test.com", "y");
        Item it1 = makeItem("F", 6000);
        Item it2 = makeItem("G", 7000);
        makeReview(m, it1, "r1", 5);
        makeReview(m2, it1, "r2", 3);
        makeReview(m, it2, "r3", 4);

        var rows = reviewRepository.findReviewCountByItemIds(List.of(it1.getId(), it2.getId()));
        Map<Long, Long> map = rows.stream().collect(
                java.util.stream.Collectors.toMap(
                        ReviewRepository.ItemReviewCount::getItemId,
                        ReviewRepository.ItemReviewCount::getReviewCount
                )
        );
        assertEquals(2L, map.get(it1.getId()));
        assertEquals(1L, map.get(it2.getId()));
    }
}
