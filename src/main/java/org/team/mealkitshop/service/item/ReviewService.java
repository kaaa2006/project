// src/main/java/org/team/mealkitshop/service/item/ReviewService.java
package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.dto.item.ReviewReplyDTO;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.item.ReviewReplyRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.repository.order.OrderRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final ItemImgRepository itemImgRepository;
    private final ReviewImageService reviewImageService;
    private final ReviewReplyRepository reviewReplyRepository;
    private final OrderRepository orderRepository;

    /* ==================== CREATE ==================== */
    @Transactional
    public Long create(ReviewDTO dto, List<MultipartFile> images) {
        Objects.requireNonNull(dto, "dto is null");
        Long writerMno = Objects.requireNonNull(dto.getWriterMno(), "writerMno is null");
        Long itemId = Objects.requireNonNull(dto.getItemId(), "itemId is null");

        Member writer = memberRepository.findById(writerMno)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. mno=" + writerMno));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + itemId));

        // 구매 이력(배송완료 이상) 확인
        if (!orderRepository.hasPurchasedDeliveredOrLater(writerMno, itemId)) {
            throw new SecurityException("배송완료 이후부터 리뷰를 작성할 수 있습니다.");
        }



        Review review = new Review();
        review.setMember(writer);
        review.setItem(item);
        review.changeContent(Objects.requireNonNull(dto.getContent(), "content is null"));
        review.changeRating(Objects.requireNonNull(dto.getRating(), "rating is null"));

        Review saved = reviewRepository.save(review);

        if (images != null && !images.isEmpty()) {
            reviewImageService.addImages(saved.getId(), images);
        }
        return saved.getId();
    }

    /* ==================== UPDATE ==================== */
    @Transactional
    public void update(ReviewDTO patch, List<MultipartFile> newImages, boolean replaceImages, Long actorMno) {
        Objects.requireNonNull(patch, "patch is null");
        Long reviewId = Objects.requireNonNull(patch.getId(), "id is null");

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));

        if (!review.getMember().getMno().equals(actorMno)) {
            throw new SecurityException("리뷰 수정 권한이 없습니다.");
        }

        if (patch.getContent() != null) review.changeContent(patch.getContent());
        if (patch.getRating() != null) review.changeRating(patch.getRating());

        if (replaceImages) {
            reviewImageService.replaceImages(reviewId, safeList(newImages));
        } else if (newImages != null && !newImages.isEmpty()) {
            reviewImageService.addImages(reviewId, newImages);
        }
    }

    /* ==================== DELETE ==================== */
    @Transactional
    public void delete(Long reviewId) {
        Objects.requireNonNull(reviewId, "reviewId is null");

        reviewImageService.deleteByReview(reviewId);         // 이미지 메타 삭제
        reviewReplyRepository.deleteByReview_Id(reviewId);   // 관리자 답변 삭제
        reviewRepository.deleteById(reviewId);               // 리뷰 삭제
    }

    /* ==================== READ / PAGE ==================== */
    public Page<ReviewDTO> listByItem(Long itemId, Pageable pageable, boolean withImages, boolean withReply) {
        if (itemId == null) throw new IllegalArgumentException("itemId is null");
        if (pageable == null) throw new IllegalArgumentException("pageable is null");

        Page<Review> page = withImages
                ? reviewRepository.findWithImagesByItemId(itemId, pageable)
                : reviewRepository.findByItem_Id(itemId, pageable);

        return attachAndMap(page, withImages, withReply);
    }

    public Page<ReviewDTO> listByMember(Long mno, Pageable pageable, boolean withImages, boolean withReply) {
        if (mno == null) throw new IllegalArgumentException("mno is null");
        if (pageable == null) throw new IllegalArgumentException("pageable is null");

        Page<Review> page = reviewRepository.findByMember_Mno(mno, pageable);
        return attachAndMap(page, withImages, withReply);
    }

    @Transactional(readOnly = true)
    public Review getEntity(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));
    }

    @Transactional(readOnly = true)
    public ReviewDTO getDetail(Long reviewId, boolean withImages, boolean withReply) {
        Review review = reviewRepository.findWithItemAndMemberById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));

        List<ReviewImageDTO> images = withImages ? reviewImageService.listByReview(review.getId()) : List.of();
        ReviewReplyDTO replyDto = withReply
                ? reviewReplyRepository.findByReview_Id(review.getId()).map(this::toReplyDTO).orElse(null)
                : null;

        return toDTO(review, images, replyDto);
    }

    /* ==================== INTERNAL HELPERS ==================== */
    private Page<ReviewDTO> attachAndMap(Page<Review> page, boolean withImages, boolean withReply) {
        // 빈 페이지면 즉시 매핑 종료
        if (page.isEmpty()) return page.map(r -> toDTO(r, List.of(), null));

        // == 1) ID 수집 ==
        final List<Long> reviewIds = page.stream()
                .map(Review::getId)
                .toList();

        // 페이지 내 리뷰들이 참조하는 itemId(중복 제거)
        final List<Long> itemIds = page.stream()
                .map(r -> r.getItem() != null ? r.getItem().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // == 2) 배치 로딩 (이미지/답변/대표썸네일) ==
        final Map<Long, List<ReviewImageDTO>> imagesMap = withImages
                ? reviewImageService.listByReviewIdsGrouped(reviewIds)
                : Collections.emptyMap();

        final Map<Long, ReviewReplyDTO> replyMap = withReply
                ? reviewReplyRepository.findByReview_IdIn(reviewIds).stream()
                .collect(Collectors.toMap(
                        rr -> rr.getReview().getId(),
                        this::toReplyDTO,
                        (a, b) -> a
                ))
                : Collections.emptyMap();

        // 대표 썸네일(detail=false 정책)
        final Map<Long, String> itemThumbMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemImgRepository.findRepUrlsByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        ItemImgRepository.ItemRepProjection::getItemId,
                        ItemImgRepository.ItemRepProjection::getImgUrl,
                        (a, b) -> a
                ));

        // == 3) DTO 매핑 ==
        return page.map(r -> {
            final ReviewDTO dto = toDTO(
                    r,
                    imagesMap.getOrDefault(r.getId(), List.of()),
                    replyMap.get(r.getId())
            );

            // ★ 최소 수정: null 이거나 /items/... 이면 기본 썸네일로 보정
            if (dto.getItemId() != null) {
                String t = dto.getItemThumbUrl();
                if (t == null) t = itemThumbMap.get(dto.getItemId());
                if (t == null || t.startsWith("/items/")) t = "/img/No_Image.jpg";
                dto.setItemThumbUrl(t);
            }

            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Long getOwnerMno(Long reviewId) {
        var review = reviewRepository.findWithItemAndMemberById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));
        return review.getMember().getMno();
    }

    private ReviewDTO toDTO(Review r, List<ReviewImageDTO> images, ReviewReplyDTO reply) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());

        if (r.getMember() != null) {
            dto.setWriterMno(r.getMember().getMno());
            dto.setWriterName(r.getMember().getMemberName());
        }

        if (r.getItem() != null) {
            dto.setItemId(r.getItem().getId());
            dto.setItemName(r.getItem().getItemNm());
        }

        dto.setContent(r.getContent());
        dto.setRating(r.getRating());
        dto.setRegTime(r.getRegTime());
        dto.setUpdateTime(r.getUpdateTime());
        dto.setReviewImages(images != null ? images : List.of());
        dto.setReply(reply);

        return dto;
    }

    private ReviewReplyDTO toReplyDTO(ReviewReply rr) {
        ReviewReplyDTO dto = new ReviewReplyDTO();
        dto.setId(rr.getId());
        dto.setReviewId(rr.getReview().getId());
        if (rr.getAdmin() != null) {
            dto.setAdminId(rr.getAdmin().getMno());
            dto.setAdminName(rr.getAdmin().getMemberName());
        }
        dto.setContent(rr.getContent());
        dto.setRegTime(rr.getRegTime());
        dto.setUpdateTime(rr.getUpdateTime());
        return dto;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
