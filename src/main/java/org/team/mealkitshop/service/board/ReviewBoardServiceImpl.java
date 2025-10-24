package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReply;
import org.team.mealkitshop.domain.board.ReviewBoardView;
import org.team.mealkitshop.domain.item.ReviewReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.ReviewBoardDTO;
import org.team.mealkitshop.mapper.board.ReviewBoardMapper;
import org.team.mealkitshop.repository.board.ReviewBoardReactionRepository;
import org.team.mealkitshop.repository.board.ReviewBoardRepository;
import org.team.mealkitshop.repository.board.ReviewBoardViewRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;

/**
 * 리뷰 게시판 CRUD 전용 서비스 구현체
 */
@Service
@RequiredArgsConstructor
public class ReviewBoardServiceImpl implements ReviewBoardService {

    private final ReviewBoardRepository reviewBoardRepository;
    private final ReviewBoardMapper reviewBoardMapper;
    private final ReviewBoardReactionRepository reviewBoardReactionRepository;
    private final MemberRepository memberRepository;
    private final ReviewBoardViewRepository reviewBoardViewRepository;

    @Override
    @Transactional
    public Long register(ReviewBoardDTO dto) {
        // 로그인한 사용자 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인 필요");
        }

        String loginEmail = authentication.getName(); // 보통 username(email) 저장됨

        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // DTO → Entity
        ReviewBoard board = reviewBoardMapper.toEntity(dto);

        // 작성자 정보 세팅
        board.setWriterMember(member);

        ReviewBoard savedBoard = reviewBoardRepository.saveAndFlush(board);

        return savedBoard.getBno();
    }

    @Override
    public ReviewBoardDTO readOne(Long bno, Long memberId) {  // memberId 인자 추가
        ReviewBoard board = reviewBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        if (memberId != null) {  // 로그인 회원이면 조회수 증가
            incrementViewCountForMember(board, memberId);
        }

        return reviewBoardMapper.toDTO(board);
    }

    // fetch join으로 댓글과 함께 조회
    @Override
    @Transactional(readOnly = true)
    public ReviewBoard readBoardWithReplies(Long bno) {
        return reviewBoardRepository.findByIdWithReplies(bno)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. bno=" + bno));
    }

    @Override
    @Transactional
    public void modify(ReviewBoardDTO dto) {
        ReviewBoard board = reviewBoardRepository.findById(dto.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());

        // ✅ 비밀글/공개글 반영
        board.setSecretBoard(dto.isSecretBoard());
        board.setSecretPassword(dto.getSecretPassword());

        reviewBoardRepository.save(board);
    }

    @Override
    @Transactional
    public void remove(Long bno, Member member) {
        ReviewBoard board = reviewBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        // 작성자 확인
        if (!board.getWriter().equals(member.getMemberName())) {
            throw new AccessDeniedException("본인 글만 삭제 가능합니다.");
        }

        // 댓글 존재 여부 체크
        if (!board.getReplies().isEmpty()) {
            throw new IllegalStateException("댓글이 달린 리뷰는 삭제할 수 없습니다.");
        }

        // 관련 반응 삭제
        reviewBoardReactionRepository.deleteAllByReviewBoard(board);

        // 조회수 기록 삭제 (필수!)
        reviewBoardViewRepository.deleteAllByBoard(board);

        // 게시글 삭제
        reviewBoardRepository.delete(board);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewBoard> getReviewBoardList() {
        // ✅ 상위 인기글 5개
        List<ReviewBoard> topHelpful = reviewBoardRepository.findTop5ByOrderByHelpfulCountDesc();
        // ✅ 최신글 Top 50 (모든 글 조회)
        List<ReviewBoard> latest = reviewBoardRepository.findTop50ByOrderByRegTimeDesc();
        // ✅ 최신글에서 인기글 제거 (중복 방지)
        latest.removeAll(topHelpful);
        // ✅ 인기글 표시 플래그 설정
        topHelpful.forEach(board -> board.setTopHelpful(true));
        // ✅ 합치기
        topHelpful.addAll(latest);
        return topHelpful;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewBoard> getTopHelpfulReviews(int topCount) {
        List<ReviewBoard> topHelpful = reviewBoardRepository.findTopHelpful(PageRequest.of(0, topCount));
        // ✅ 조회된 글 모두 베스트 글 표시
        topHelpful.forEach(board -> board.setTopHelpful(true));
        return topHelpful;
    }

    //@PreAuthorize("hasRole('USER')")
    //@PostMapping("/reply/admin-register")
    //@ResponseBody // JSON 반환
    public ReviewBoardReply registerReply(Long bno, Member member, String replyText) {
        ReviewBoard board = reviewBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + bno));

        ReviewBoardReply reply = ReviewBoardReply.builder()
                .reviewBoard(board)
                .replyer(member)
                .replyText(replyText)
                .secret(false)
                .build();

        board.getReplies().add(reply);
        reviewBoardRepository.save(board);

        return reply; // JSON 반환용
    }

    @Override
    @Transactional
    public void incrementViewCountForMember(ReviewBoard board, Long memberId) {
        boolean alreadyViewed = reviewBoardViewRepository.existsByBoard_BnoAndMember_Mno(board.getBno(), memberId);

        if (!alreadyViewed) {
            board.setViewCount(board.getViewCount() + 1);
            reviewBoardRepository.save(board);

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

            ReviewBoardView viewRecord = ReviewBoardView.builder()
                    .board(board)
                    .member(member)
                    .build();
            reviewBoardViewRepository.save(viewRecord);
        }
    }

    @Override
    public boolean checkPassword(ReviewBoard board, String password, Member member) {
        if (member != null && member.getRole() == Role.ADMIN) { // ✅ enum 비교
            return true;
        }
        String saved = board.getSecretPassword();
        return saved != null && saved.equals(password);
    }
}