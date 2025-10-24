package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipBoardReaction;
import org.team.mealkitshop.domain.board.TipBoardView;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.TipBoardDTO;
import org.team.mealkitshop.mapper.board.TipBoardMapper;
import org.team.mealkitshop.repository.board.TipBoardReactionRepository;
import org.team.mealkitshop.repository.board.TipBoardRepository;
import org.team.mealkitshop.repository.board.TipBoardViewRepository;
import org.team.mealkitshop.repository.board.TipReplyRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TIP 게시판 CRUD 전용 서비스 구현체
 * - 로그인 회원 기준 작성/수정/삭제 처리
 * - 최신 TIP, 상위 좋아요 TIP 조회 제공
 * - 좋아요/싫어요 반응 토글 처리
 */
@Service
@RequiredArgsConstructor
public class TipBoardServiceImpl implements TipBoardService {

    private final TipBoardRepository tipBoardRepository;
    private final TipBoardReactionRepository tipBoardReactionRepository;
    private final TipBoardViewRepository tipBoardViewRepository;
    private final TipReplyRepository tipReplyRepository;

    private final TipBoardMapper tipBoardMapper;

    /** TIP 등록 */
    @Override
    @Transactional
    public Long register(TipBoardDTO dto, Member member) {
        // 작성자 강제 설정
        dto.setWriter(member.getMemberName());
        dto.setWriterId(member.getMno());

        // DTO → Entity 변환
        TipBoard board = tipBoardMapper.toEntity(dto);

        return tipBoardRepository.save(board).getBno();
    }

    /** 단건 조회 */
    @Override
    public TipBoardDTO readOne(Long bno, String userId) {
        TipBoard tipBoard = tipBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        // 로그인 회원이면 조회수 증가
        if (userId != null) {
            Long memberId = Long.parseLong(userId); // userId가 MemberId라면 그대로
            incrementViewCountForMember(tipBoard, memberId);
        }

        // ModelMapper 적용
        TipBoardDTO tipBoardDTO = tipBoardMapper.toDTO(tipBoard);
        tipBoardDTO.setViewCount(tipBoard.getViewCount()); // viewCount는 skip 처리했으므로 별도 세팅 필요

        // 좋아요/싫어요 체크
        if (userId != null) {
            tipBoardReactionRepository.findByTipBoardAndUserId(tipBoard, userId).ifPresentOrElse(
                    reaction -> {
                        tipBoardDTO.setAlreadyAddLike(reaction.getReaction() == BoardReactionType.LIKE);
                        tipBoardDTO.setAlreadyAddDislike(reaction.getReaction() == BoardReactionType.DIS_LIKE);
                    },
                    () -> {
                        tipBoardDTO.setAlreadyAddLike(false);
                        tipBoardDTO.setAlreadyAddDislike(false);
                    }
            );
        } else {
            tipBoardDTO.setAlreadyAddLike(false);
            tipBoardDTO.setAlreadyAddDislike(false);
        }

        return tipBoardDTO;
    }

    /** 회원별 조회수 증가 */
    @Override
    @Transactional
    public void incrementViewCountForMember(TipBoard board, Long memberId) {
        boolean alreadyViewed = tipBoardViewRepository.existsByTipBoard_BnoAndMember_Mno(board.getBno(), memberId);

        if (!alreadyViewed) {
            board.setViewCount(board.getViewCount() + 1);
            tipBoardRepository.save(board);

            Member member = new Member();
            member.setMno(memberId); // Member 객체 간단 생성 (Repository 조회 가능)

            TipBoardView viewRecord = TipBoardView.builder()
                    .tipBoard(board)
                    .member(member)
                    .build();
            tipBoardViewRepository.save(viewRecord);
        }
    }

    /** 게시글 수정 (본인만) */
    @Override
    @Transactional
    public void modify(TipBoardDTO dto, Member member) {
        TipBoard board = tipBoardRepository.findById(dto.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        if (!board.getWriter().equals(member.getMemberName())) {
            throw new AccessDeniedException("본인 글만 수정 가능합니다.");
        }

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());

        // ✅ writerId도 보장
        board.setWriterId(member.getMno());

        tipBoardRepository.save(board);
    }

    /** 게시글 삭제 (본인만) */
    @Override
    @Transactional
    public void remove(Long bno, Member member) {
        TipBoard board = tipBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        if (!board.getWriter().equals(member.getMemberName())) {
            throw new AccessDeniedException("본인 글만 삭제 가능합니다.");
        }

        // 관련 반응 삭제
        tipBoardReactionRepository.deleteAllByTipBoard(board);

        // 관련 댓글 삭제
        tipReplyRepository.deleteAllByTipBoard(board);

        // 게시글 삭제
        tipBoardRepository.delete(board);
    }

    /** 상위 좋아요 TIP 조회 */
    @Override
    public List<TipBoard> getTopLikedTips(int topCount) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<TipBoard> list = tipBoardRepository.findTop5ByRegTimeAfterOrderByLikeCountDesc(oneMonthAgo);
        return list.size() > topCount ? list.subList(0, topCount) : list;
    }

    /** 최신 TIP 조회 */
    @Override
    public List<TipBoard> getRecentTips() {
        // 1. 모든 글 조회 (좋아요/조회수 등은 그대로)
        List<TipBoard> allTips = tipBoardRepository.findAllByOrderByRegTimeDesc();

        // 2. 최근 한 달 기준 좋아요 상위 5개 조회
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Long> topLikedBnos = tipBoardRepository
                .findTop5ByRegTimeAfterOrderByLikeCountDesc(oneMonthAgo)
                .stream()
                .map(TipBoard::getBno)
                .toList();

        // 최신글 그대로 두되, topLiked 여부는 DTO에서 표시 가능
        return allTips.stream()
                .peek(board -> board.setTopLiked(topLikedBnos.contains(board.getBno())))
                .toList();
    }

    /** 좋아요/싫어요 토글 */
    @Override
    @Transactional
    public void toggleReaction(Long bno, String userId, BoardReactionType type) {
        TipBoard board = tipBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        Optional<TipBoardReaction> existing = tipBoardReactionRepository.findByTipBoardAndUserId(board, userId);

        if (existing.isPresent()) {
            TipBoardReaction reaction = existing.get();
            if (reaction.getReaction() == type) {
                // 같은 버튼이면 취소
                tipBoardReactionRepository.delete(reaction);
            } else {
                // 다른 버튼이면 교체
                reaction.setReaction(type);
                tipBoardReactionRepository.save(reaction);
            }
        } else {
            // 새로 추가
            TipBoardReaction reaction = new TipBoardReaction();
            reaction.setTipBoard(board);
            reaction.setUserId(userId);
            reaction.setReaction(type);
            tipBoardReactionRepository.save(reaction);
        }

        // TIP 게시글에 좋아요/싫어요 수 업데이트
        board.setLikeCount(tipBoardReactionRepository.countByTipBoardAndReaction(board, BoardReactionType.LIKE));
        board.setDislikeCount(tipBoardReactionRepository.countByTipBoardAndReaction(board, BoardReactionType.DIS_LIKE));
        tipBoardRepository.save(board);
    }
}