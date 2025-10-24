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
import org.team.mealkitshop.domain.item.ItemImage;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.ListItemDTO;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ItemRepositoryCustomTests {

    @Autowired ItemRepository itemRepository;
    @Autowired ItemImgRepository itemImgRepository;

    @PersistenceContext  // 또는 @Autowired 도 가능
    private EntityManager em;

    @BeforeEach
    void seedMainItems() {
        // 이미 데이터가 있다면 건너뛰고 싶으면 조건 추가 가능
        for (int i = 1; i <= 10; i++) {
            Item item = Item.builder()
                    .itemNm("프리미엄 세트 " + i) // ★ 검색어 "세트" 포함
                    .price(10000 + i)
                    .stockNumber(50 + i)
                    .itemDetail("신선한 재료로 구성된 세트 " + i)
                    .itemSellStatus(ItemSellStatus.SELL)
                    .category(Category.SET)         // (필터가 있다면 조건에 맞게)
                    .foodItem(FoodItem.SET)         // (선택)
                    .build();
            item = itemRepository.save(item);

            // 대표 이미지(반드시 true) — BooleanToYNConverter가 "Y"로 저장
            ItemImage rep = new ItemImage();
            rep.setItem(item);
            rep.setOriImgName("set" + i + ".jpg");
            rep.setImgName("set" + i + "_rep.jpg");
            rep.setImgUrl("/img/set" + i + "_rep.jpg");
            rep.setRepimgYn(Boolean.TRUE);   // ★ 핵심
            itemImgRepository.save(rep);

            // (선택) 비대표 이미지
            ItemImage sub = new ItemImage();
            sub.setItem(item);
            sub.setOriImgName("set" + i + "_sub.jpg");
            sub.setImgName("set" + i + "_sub.jpg");
            sub.setImgUrl("/img/set" + i + "_sub.jpg");
            sub.setRepimgYn(Boolean.FALSE);
            itemImgRepository.save(sub);
        }
        em.flush();
        em.clear();
    }

    @Test @DisplayName("Admin 페이지 쿼리(getAdminItemPage)")
    void getAdminItemPage() {
        ItemSearchDTO cond = new ItemSearchDTO();
        cond.setSearchQuery("세트"); // 이름 검색 조건 존재 시 필터링
        var page = itemRepository.getAdminItemPage(cond, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(10);
        assertThat(page.getContent().get(0).getItemNm()).contains("세트");
    }

    @Test @DisplayName("메인 페이지 쿼리(getMainItemPage)")
    void getMainItemPage() {
        ItemSearchDTO cond = new ItemSearchDTO();
        cond.setSearchQuery("세트");
        var page = itemRepository.getListItemPage(cond, PageRequest.of(0, 8));

        assertThat(page.getContent().size()).isEqualTo(8);
        // MainItemDTO 매핑 필수 필드 확인
        ListItemDTO dto = page.getContent().get(0);
        assertThat(dto.getItemNm()).contains("세트");
        assertThat(dto.getPrice()).isNotNull();
        assertThat(dto.getId()).isNotNull();
    }
}
