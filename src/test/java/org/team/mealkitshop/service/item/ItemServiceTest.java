package org.team.mealkitshop.service.item;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemImage;
import org.team.mealkitshop.dto.item.*;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemLikeRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ItemService 단위 테스트 (Mockito)
 * - create/read/list/listWithStats/update/delete/getAdminPage/getListPage
 * - XSS 정화, 배치 통계, 파일 삭제, 커스텀 리포 위임 검증
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemImgRepository itemImgRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ItemLikeRepository itemLikeRepository;
    @Mock private FileService fileService;

    @InjectMocks
    private ItemService itemService;

    /* ========================= helpers ========================= */

    private Item sampleItem(Long id) {
        Item i = Item.builder()
                .id(id)
                .itemNm("샐러드")
                .originalPrice(9900)
                .stockNumber(100)
                .itemDetail("상세")
                .itemSellStatus(null)
                .itemLike(5L)
                .itemViewCnt(123L)
                .build();
        // BaseEntity 필드 세팅 흉내
        try {
            var regField = Item.class.getSuperclass().getDeclaredField("regTime");
            regField.setAccessible(true);
            regField.set(i, LocalDateTime.now());
            var updField = Item.class.getSuperclass().getDeclaredField("updateTime");
            updField.setAccessible(true);
            updField.set(i, LocalDateTime.now());
        } catch (Exception ignore) {}
        return i;
    }

    private ItemImage img(Long id, Long itemId, String url, boolean rep) {
        ItemImage img = new ItemImage();
        img.setId(id);
        img.setImgName("saved-"+id+".jpg");
        img.setOriImgName("ori-"+id+".jpg");
        img.setImgUrl(url);
        //img.setRepimgYn(rep);
        Item item = new Item();
        item.setId(itemId);
        img.setImgUrl(img.getImgUrl());
        return img;
    }

    /* ========================= create ========================= */

    @Test
    void create_저장전_HTML_정화_및_ID반환() {
        // given
        ItemFormDTO dto = new ItemFormDTO();
        dto.setItemNm("샐러드");
        dto.setItemDetail("""
                <p>안전한 <b>설명</b><script>alert(1)</script>
                <img src="http://example.com/a.jpg" onerror="hack()" /></p>
                """);

        // ItemFormDTO#createItem() 를 호출하므로, save 시점에 반환할 엔티티 준비
        Item saved = sampleItem(10L);
        given(itemRepository.save(any(Item.class))).willReturn(saved);

        // when
        Long id = itemService.create(dto);

        // then
        assertThat(id).isEqualTo(10L);
        // 저장된 엔티티를 캡처하여 itemDetail 이 정화되었는지 확인
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        String cleaned = captor.getValue().getItemDetail();
        assertThat(cleaned).doesNotContain("<script>");
        assertThat(cleaned).contains("<img"); // 이미지 허용
        assertThat(cleaned).doesNotContain("onerror");
    }

    /* ========================= read ========================= */

    @Test
    void read_상세조회_리뷰통계_이미지정렬_포함() {
        // given
        Long itemId = 1L;
        Item entity = sampleItem(itemId);
        given(itemRepository.findDetailBundleById(itemId)).willReturn(Optional.of(entity));

        // 대표 먼저 → 그 다음 등록순
        List<ItemImage> ordered = List.of(
                img(2L, itemId, "/images/rep.jpg", true),
                img(3L, itemId, "/images/other1.jpg", false),
                img(4L, itemId, "/images/other2.jpg", false)
        );
        given(itemImgRepository.findAllForDetail(itemId)).willReturn(ordered);

        given(reviewRepository.getAverageRatingByItemId(itemId)).willReturn(4.5);
        given(reviewRepository.countByItem_Id(itemId)).willReturn(7L);

        // when
        ItemDTO dto = itemService.read(itemId);

        // then
        assertThat(dto.getId()).isEqualTo(itemId);
        assertThat(dto.getAvgRating()).isEqualTo(4.5);
        assertThat(dto.getReviewCount()).isEqualTo(7L);
        assertThat(dto.getItemImages()).hasSize(3);
        assertThat(dto.getItemImages().get(0).getImgUrl()).isEqualTo("/images/rep.jpg");
    }

    @Test
    void read_존재하지않으면_예외() {
        given(itemRepository.findDetailBundleById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.read(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /* ========================= list ========================= */

    @Test
    void list_엔티티_to_DTO_매핑만_수행() {
        // given
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
        List<Item> content = List.of(sampleItem(1L), sampleItem(2L));
        Page<Item> page = new PageImpl<>(content, pageable, 10);
        given(itemRepository.findAll(pageable)).willReturn(page);

        // when
        Page<ItemDTO> result = itemService.list(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getContent()).extracting(ItemDTO::getId).containsExactly(1L, 2L);
        // 통계는 포함되지 않음
        assertThat(result.getContent().get(0).getAvgRating()).isNull();
    }

    /* ========================= listWithStats ========================= */

    @Test
    void listWithStats_배치로_평균과_개수_채우기() {
        // given
        Pageable pageable = PageRequest.of(0, 3);
        List<Item> content = List.of(sampleItem(1L), sampleItem(2L), sampleItem(3L));
        Page<Item> page = new PageImpl<>(content, pageable, 3);
        given(itemRepository.findAll(pageable)).willReturn(page);

        // 평균/개수 배치 쿼리 결과 모의
        var avgRows = List.of(
                new ReviewRepository.ItemAvgRating() {
                    public Long getItemId() { return 1L; }
                    public Double getAvgRating() { return 4.0; }
                },
                new ReviewRepository.ItemAvgRating() {
                    public Long getItemId() { return 3L; }
                    public Double getAvgRating() { return 0.0; }
                }
        );
        var cntRows = List.of(
                new ReviewRepository.ItemReviewCount() {
                    public Long getItemId() { return 1L; }
                    public Long getReviewCount() { return 5L; }
                },
                new ReviewRepository.ItemReviewCount() {
                    public Long getItemId() { return 2L; }
                    public Long getReviewCount() { return 2L; }
                }
        );

        given(reviewRepository.findAvgRatingByItemIds(List.of(1L,2L,3L))).willReturn(avgRows);
        given(reviewRepository.findReviewCountByItemIds(List.of(1L,2L,3L))).willReturn(cntRows);

        // when
        Page<ItemDTO> result = itemService.listWithStats(pageable);

        // then
        Map<Long, ItemDTO> map = result.stream().collect(
                java.util.stream.Collectors.toMap(ItemDTO::getId, v -> v));

        assertThat(map.get(1L).getAvgRating()).isEqualTo(4.0);
        assertThat(map.get(1L).getReviewCount()).isEqualTo(5L);

        // 평균 누락 → 0.0 보정
        assertThat(map.get(2L).getAvgRating()).isEqualTo(0.0);
        assertThat(map.get(2L).getReviewCount()).isEqualTo(2L);

        assertThat(map.get(3L).getAvgRating()).isEqualTo(0.0);
        assertThat(map.get(3L).getReviewCount()).isEqualTo(0L);
    }

    /* ========================= update ========================= */

    @Test
    void update_엔티티더티체킹_및_HTML_정화() {
        // given
        ItemFormDTO dto = new ItemFormDTO();
        dto.setItemDetail("<p>ok<script>alert(1)</script></p>");

        Item entity = sampleItem(7L);
        given(itemRepository.findById(7L)).willReturn(Optional.of(entity));

        // when
        ItemDTO updated = itemService.update(7L, dto);

        // then
        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(entity.getItemDetail()).doesNotContain("<script>");
        verify(itemRepository, never()).save(any()); // 더티체킹만
    }

    /* ========================= delete ========================= */

    @Test
    void delete_파일삭제_좋아요정리_리뷰삭제_엔티티삭제() throws IOException {
        // given
        Long itemId = 77L;
        Item entity = sampleItem(itemId);
        given(itemRepository.findById(itemId)).willReturn(Optional.of(entity));

        List<ItemImage> images = List.of(
                img(1L, itemId, "/u/1.jpg", true),
                img(2L, itemId, "/u/2.jpg", false)
        );
        given(itemImgRepository.findByItemIdOrderByIdAsc(itemId)).willReturn(images);

        // when
        itemService.delete(itemId);

        // then
        // 파일 삭제 시도
        verify(fileService, times(2)).deleteBySavedName(anyString());
        // 좋아요 정리
        verify(itemLikeRepository).deleteByItem_Id(itemId);
        // 리뷰 삭제
        verify(reviewRepository).deleteByItemId(itemId);
        // 최종 삭제
        verify(itemRepository).delete(entity);
    }

    @Test
    void delete_없으면_예외() {
        given(itemRepository.findById(404L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.delete(404L))
                .isInstanceOf(NoSuchElementException.class);
    }

    /* ========================= paging(위임) ========================= */

    @Test
    void getAdminPage_커스텀리포지토리위임() {
        ItemSearchDTO cond = new ItemSearchDTO();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ListItemDTO> page = new PageImpl<>(List.of(sampleItem(1L)), pageable, 1);

        given(itemRepository.getAdminItemPage(cond, pageable)).willReturn(page);

        Page<ListItemDTO> result = itemService.getAdminPage(cond, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(itemRepository).getAdminItemPage(cond, pageable);
    }

    @Test
    void getListPage_커스텀DTO프로젝션위임() {
        ItemSearchDTO cond = new ItemSearchDTO();
        Pageable pageable = PageRequest.of(0, 8);

        ListItemDTO row = ListItemDTO.builder()
                .id(1L)
                .itemNm("샐러드")
                .itemDetail("설명")
                .repImgUrl("/images/x.jpg")
                .price(9900)
                .itemLike(1L)
                .itemViewCnt(10L)
                .reviewCount(3L)
                .avgRating(4.5)
                .build();

        Page<ListItemDTO> page = new PageImpl<>(List.of(row), pageable, 1);
        given(itemRepository.getListItemPage(cond, pageable)).willReturn(page);

        Page<ListItemDTO> result = itemService.getListPage(cond, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRepImgUrl()).isEqualTo("/images/x.jpg");
        verify(itemRepository).getListItemPage(cond, pageable);
    }

    /* ========================= read-only 보장 체크 ========================= */

    @Nested
    class ReadOnlyFlags {
        @Test
        void listWithStats_읽기전용_변경쿼리없음() {
            Pageable pageable = PageRequest.of(0, 1);
            Page<Item> page = new PageImpl<>(List.of(sampleItem(1L)), pageable, 1);
            given(itemRepository.findAll(pageable)).willReturn(page);
            given(reviewRepository.findAvgRatingByItemIds(anyList())).willReturn(List.of());
            given(reviewRepository.findReviewCountByItemIds(anyList())).willReturn(List.of());

            Page<ItemDTO> result = itemService.listWithStats(pageable);
            assertThat(result.getTotalElements()).isEqualTo(1);
            // 변경 계열 메서드 호출 없는지(간단 검증)
            verify(itemRepository, never()).delete(any());
            verify(itemRepository, never()).save(any());
        }
    }
}
