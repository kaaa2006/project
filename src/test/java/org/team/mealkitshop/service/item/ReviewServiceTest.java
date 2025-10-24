package org.team.mealkitshop.service.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.dto.item.ReviewReplyDTO;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewReplyRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceTest {

    @MockitoBean
    private ReviewRepository reviewRepository;
    @MockitoBean private MemberRepository memberRepository;
    @MockitoBean private ItemRepository itemRepository;
    @MockitoBean private ReviewImageService reviewImageService;
    @MockitoBean private ReviewReplyRepository reviewReplyRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository,
                memberRepository,
                itemRepository,
                reviewImageService,
                reviewReplyRepository
        );
    }

    /* ========================= CREATE ========================= */

    @Test
    @DisplayName("create: 이미지가 없으면 addImages 호출 안 함")
    void create_review_without_images() {
        // given
        ReviewDTO dto = new ReviewDTO();
        dto.setWriterMno(1L);
        dto.setItemId(100L);
        dto.setContent("맛있어요!");
        dto.setRating(5);

        Member writer = member(1L, "홍길동");
        Item item = item(100L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(writer));
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        // when
        Long id = reviewService.create(dto, List.of());

        // then
        assertThat(id).isEqualTo(10L);
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewImageService, never()).addImages(anyLong(), anyList());
    }

    @Test
    @DisplayName("create: 이미지가 있으면 addImages 호출")
    void create_review_with_images() {
        // given
        ReviewDTO dto = new ReviewDTO();
        dto.setWriterMno(2L);
        dto.setItemId(101L);
        dto.setContent("괜찮아요");
        dto.setRating(4);

        when(memberRepository.findById(2L)).thenReturn(Optional.of(member(2L, "김철수")));
        when(itemRepository.findById(101L)).thenReturn(Optional.of(item(101L)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(11L);
            return r;
        });

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("a.jpg");

        // when
        Long id = reviewService.create(dto, List.of(mockFile));

        // then
        assertThat(id).isEqualTo(11L);
        verify(reviewImageService).addImages(eq(11L), anyList());
    }

    /* ========================= UPDATE ========================= */

    @Test
    @DisplayName("update: 본인 리뷰만 수정 가능, 내용/평점 패치")
    void update_review_patch_fields() {
        // given
        Review existing = reviewWith(20L, member(1L, "작성자"), item(200L));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(existing));

        ReviewDTO patch = new ReviewDTO();
        patch.setId(20L);
        patch.setWriterMno(1L);
        patch.setContent("업데이트된 내용");
        patch.setRating(3);

        // when
        reviewService.update(patch, List.of(), false);

        // then
        assertThat(existing.getContent()).isEqualTo("업데이트된 내용");
        assertThat(existing.getRating()).isEqualTo(3);
        verify(reviewImageService, never()).replaceImages(anyLong(), anyList());
        verify(reviewImageService, never()).addImages(anyLong(), anyList());
    }

    @Test
    @DisplayName("update: replaceImages=true면 기존 전부 교체 로직 호출")
    void update_review_replace_images() {
        Review existing = reviewWith(21L, member(5L, "작성자"), item(201L));
        when(reviewRepository.findById(21L)).thenReturn(Optional.of(existing));

        ReviewDTO patch = new ReviewDTO();
        patch.setId(21L);
        patch.setWriterMno(5L);
        patch.setContent("컨텐츠");
        patch.setRating(5);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("b.jpg");

        // when
        reviewService.update(patch, List.of(mockFile), true);

        // then
        verify(reviewImageService).replaceImages(eq(21L), anyList());
        verify(reviewImageService, never()).addImages(anyLong(), anyList());
    }

    @Test
    @DisplayName("update: replaceImages=false && 새 이미지 존재 시 addImages 호출")
    void update_review_add_images_only() {
        Review existing = reviewWith(22L, member(7L, "작성자"), item(202L));
        when(reviewRepository.findById(22L)).thenReturn(Optional.of(existing));

        ReviewDTO patch = new ReviewDTO();
        patch.setId(22L);
        patch.setWriterMno(7L);
        patch.setRating(4);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("c.jpg");

        reviewService.update(patch, List.of(mockFile), false);

        verify(reviewImageService).addImages(eq(22L), anyList());
        verify(reviewImageService, never()).replaceImages(anyLong(), anyList());
    }

    /* ========================= DELETE ========================= */

    @Test
    @DisplayName("delete: 이미지 → 답변 → 리뷰 순으로 연관 정리 호출")
    void delete_review_cascades() {
        Long reviewId = 30L;
        reviewService.delete(reviewId);
        verify(reviewImageService).deleteByReview(30L);
        verify(reviewReplyRepository).deleteByReview_Id(30L);
        verify(reviewRepository).deleteById(30L);
    }

    /* ========================= READ (PAGE/DETAIL) ========================= */

    @Test
    @DisplayName("listByItem: withImages/withReply 적용하여 DTO 매핑")
    void listByItem_with_flags() {
        Long itemId = 300L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        Review r1 = reviewWith(41L, member(1L, "A"), item(itemId));
        Review r2 = reviewWith(42L, member(2L, "B"), item(itemId));
        Page<Review> page = new PageImpl<>(List.of(r1, r2), pageable, 2);

        when(reviewRepository.findWithImagesByItemId(eq(itemId), any())).thenReturn(page);

        Map<Long, List<ReviewImageDTO>> imagesMap = new HashMap<>();
        imagesMap.put(41L, List.of(imgDto(1001L, "a.jpg")));
        imagesMap.put(42L, List.of(imgDto(1002L, "b.jpg")));
        when(reviewImageService.listByReviewIdsGrouped(List.of(41L, 42L))).thenReturn(imagesMap);

        var rr1 = replyDto(9001L, 41L, 101L, "관리자1", "답변1");
        when(reviewReplyRepository.findByReview_IdIn(List.of(41L, 42L))).thenReturn(
                List.of(toEntity(rr1))
        );

        var dtoPage = reviewService.listByItem(itemId, pageable, true, true);

        assertThat(dtoPage.getContent()).hasSize(2);
        var dto1 = dtoPage.getContent().get(0);
        var dto2 = dtoPage.getContent().get(1);

        assertThat(dto1.getId()).isEqualTo(41L);
        assertThat(dto1.getReviewImages()).extracting(ReviewImageDTO::getImgName).containsExactly("a.jpg");
        assertThat(dto1.getReply()).isNotNull();
        assertThat(dto1.getReply().getContent()).isEqualTo("답변1");

        assertThat(dto2.getId()).isEqualTo(42L);
        assertThat(dto2.getReviewImages()).extracting(ReviewImageDTO::getImgName).containsExactly("b.jpg");
        assertThat(dto2.getReply()).isNull();

        verify(reviewRepository).findWithImagesByItemId(eq(itemId), any());
        verify(reviewImageService).listByReviewIdsGrouped(List.of(41L, 42L));
        verify(reviewReplyRepository).findByReview_IdIn(List.of(41L, 42L));
    }

    @Test
    @DisplayName("listByMember: withImages/withReply 플래그에 맞춰 부가조회")
    void listByMember_with_flags() {
        Long mno = 777L;
        Pageable pageable = PageRequest.of(0, 5);

        Review r1 = reviewWith(51L, member(mno, "작성자"), item(1000L));
        Review r2 = reviewWith(52L, member(mno, "작성자"), item(1001L));
        Page<Review> page = new PageImpl<>(List.of(r1, r2), pageable, 2);

        when(reviewRepository.findByMember_Mno(mno, pageable)).thenReturn(page);

        Map<Long, List<ReviewImageDTO>> imagesMap = Map.of(
                51L, List.of(imgDto(1101L, "x.png")),
                52L, List.of(imgDto(1102L, "y.png"))
        );
        when(reviewImageService.listByReviewIdsGrouped(List.of(51L, 52L))).thenReturn(imagesMap);

        var rr = replyDto(9901L, 51L, 900L, "관리자", "확인했습니다");
        when(reviewReplyRepository.findByReview_IdIn(List.of(51L, 52L)))
                .thenReturn(List.of(toEntity(rr)));

        var dtoPage = reviewService.listByMember(mno, pageable, true, true);

        assertThat(dtoPage.getTotalElements()).isEqualTo(2);
        var map = dtoPage.getContent().stream().collect(Collectors.toMap(ReviewDTO::getId, d -> d));
        assertThat(map.get(51L).getReviewImages()).hasSize(1);
        assertThat(map.get(52L).getReviewImages()).hasSize(1);
        assertThat(map.get(51L).getReply()).isNotNull();
        assertThat(map.get(52L).getReply()).isNull();

        verify(reviewRepository).findByMember_Mno(mno, pageable);
        verify(reviewImageService).listByReviewIdsGrouped(List.of(51L, 52L));
        verify(reviewReplyRepository).findByReview_IdIn(List.of(51L, 52L));
    }

    @Test
    @DisplayName("getDetail: withImages/withReply 옵션에 따라 세부 조회")
    void get_detail_with_flags() {
        Review r = reviewWith(61L, member(1L, "작성자"), item(2000L));

        when(reviewRepository.findById(61L)).thenReturn(Optional.of(r));
        when(reviewImageService.listByReview(61L)).thenReturn(List.of(
                imgDto(1201L, "z.jpg")
        ));
        when(reviewReplyRepository.findByReview_Id(61L)).thenReturn(
                Optional.of(toEntity(replyDto(8801L, 61L, 1L, "관리자", "안내드립니다")))
        );

        var dto = reviewService.getDetail(61L, true, true);

        assertThat(dto.getId()).isEqualTo(61L);
        assertThat(dto.getReviewImages()).extracting(ReviewImageDTO::getImgName).containsExactly("z.jpg");
        assertThat(dto.getReply()).isNotNull();
        assertThat(dto.getReply().getContent()).isEqualTo("안내드립니다");

        verify(reviewRepository).findById(61L);
        verify(reviewImageService).listByReview(61L);
        verify(reviewReplyRepository).findByReview_Id(61L);
    }

    /* ========================= HELPERS ========================= */

    private Member member(Long mno, String name) {
        Member m = new Member();
        m.setMno(mno);
        m.setMemberName(name);
        return m;
    }

    private Item item(Long id) {
        Item i = new Item();
        i.setId(id);
        return i;
    }

    private Review reviewWith(Long id, Member member, Item item) {
        Review r = new Review();
        r.setId(id);
        r.setMember(member);
        r.setItem(item);
        r.changeContent("초기 내용");
        r.changeRating(5);
        return r;
    }

    private ReviewImageDTO imgDto(Long id, String name) {
        return ReviewImageDTO.builder()
                .id(id)
                .imgName(name)
                .oriImgName(name)
                .imgUrl("/" + name)
                .build();
    }

    private ReviewReplyDTO replyDto(Long id, Long reviewId, Long adminId, String adminName, String content) {
        ReviewReplyDTO dto = new ReviewReplyDTO();
        dto.setId(id);
        dto.setReviewId(reviewId);
        dto.setAdminId(adminId);
        dto.setAdminName(adminName);
        dto.setContent(content);
        return dto;
    }

    private org.team.mealkitshop.domain.item.ReviewReply toEntity(ReviewReplyDTO d) {
        return org.team.mealkitshop.domain.item.ReviewReply.builder()
                .id(d.getId())
                .review(reviewWith(d.getReviewId(), member(d.getAdminId(), d.getAdminName()), item(0L)))
                .admin(member(d.getAdminId(), d.getAdminName()))
                .content(d.getContent())
                .build();
    }
}
