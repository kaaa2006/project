package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewReply;

import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.item.ReviewReplyDTO;
import org.team.mealkitshop.repository.item.ReviewReplyRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewReplyService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository replyRepository;
    private final MemberRepository memberRepository;

    /** 관리자 권한 확인 */
    private void ensureAdmin(Member actor) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 수행할 수 있습니다.");
        }
    }

    private Member getAdminOrThrow(Long actorMemberId) {
        return memberRepository.findById(actorMemberId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다. id=" + actorMemberId));
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));
    }

    /** 내용 검증 및 정규화 */
    private String normalizeAndValidateContent(String content) {
        if (content == null) throw new IllegalArgumentException("내용이 비어 있습니다.");
        String trimmed = content.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("내용이 공백일 수 없습니다.");
        return trimmed;
    }

    /** 답변 작성 */
    public ReviewReplyDTO create(Long reviewId, Long actorMemberId, String content) {
        Member actor = getAdminOrThrow(actorMemberId);
        ensureAdmin(actor);

        Review review = getReviewOrThrow(reviewId);

        if (replyRepository.existsByReview_Id(reviewId)) {
            throw new IllegalStateException("이미 이 리뷰에 대한 답변이 존재합니다.");
        }

        try {
            ReviewReply saved = replyRepository.save(
                    ReviewReply.builder()
                            .review(review)
                            .admin(actor)
                            .content(normalizeAndValidateContent(content))
                            .build()
            );
            return toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 조건 경쟁 상황 방어
            throw new IllegalStateException("이미 이 리뷰에 대한 답변이 존재합니다.", e);
        }
    }

    /** 답변 수정 */
    public ReviewReplyDTO update(Long reviewId, Long actorMemberId, String content) {
        Member actor = getAdminOrThrow(actorMemberId);
        ensureAdmin(actor);

        ReviewReply rr = replyRepository.findByReview_Id(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰의 답변이 없습니다. id=" + reviewId));

        rr.setContent(normalizeAndValidateContent(content));
        return toDTO(rr);
    }

    /** 답변 삭제 */
    public void delete(Long reviewId, Long actorMemberId) {
        Member actor = getAdminOrThrow(actorMemberId);
        ensureAdmin(actor);
        replyRepository.deleteByReview_Id(reviewId); // 존재하지 않아도 예외 없음
    }

    /** 답변 존재 시 삭제 (타 서비스에서 사용) */
    public void deleteIfExists(Long reviewId) {
        replyRepository.deleteByReview_Id(reviewId);
    }

    /** 답변 조회 */
    @Transactional(readOnly = true)
    public ReviewReplyDTO getByReviewId(Long reviewId) {
        return replyRepository.findByReview_Id(reviewId)
                .map(this::toDTO)
                .orElse(null);
    }

    /** 엔티티 → DTO 변환 */
    private ReviewReplyDTO toDTO(ReviewReply e) {
        return ReviewReplyDTO.builder()
                .id(e.getId())
                .reviewId(e.getReview().getId())
                .adminId(e.getAdmin().getMno())
                .adminName(e.getAdmin().getMemberName())
                .content(e.getContent())
                .regTime(e.getRegTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
