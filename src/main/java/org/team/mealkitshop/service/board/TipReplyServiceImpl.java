package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.TipReplyDTO;
import org.team.mealkitshop.repository.board.TipBoardRepository;
import org.team.mealkitshop.repository.board.TipReplyRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ======================================
 * 💡 팁 게시판 댓글 서비스 구현체
 * --------------------------------------
 * - 댓글 등록, 조회, 수정, 삭제 처리
 * - 작성자 Member 기반
 * - DTO ↔ Entity 변환 처리
 * ======================================
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class TipReplyServiceImpl implements TipReplyService {

    private final TipReplyRepository tipReplyRepository;
    private final TipBoardRepository tipBoardRepository;
    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;

    /**
     * 댓글 등록
     * - 로그인된 사용자 ID 기반 작성자 설정
     * - TipBoard 존재 여부 체크
     * - 양방향 연관관계 자동 설정
     */
    @Override
    @Transactional
    public Long addReply(TipReplyDTO dto) {
        Member writer = memberRepository.findById(dto.getWriterId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        TipBoard tipBoard = tipBoardRepository.findById(dto.getTipBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        TipReply reply = new TipReply();
        reply.setTipBoard(tipBoard);    // 양방향 연관관계
        reply.setReplyer(writer);       // 작성자 설정
        reply.changeText(dto.getReplyText());

        return tipReplyRepository.save(reply).getRno();
    }

    /**
     * 특정 게시글 댓글 조회 (페이징)
     * - 작성자 이름(writerName)과 ID(writerId) DTO에 채움
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TipReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO) {

        // Repository에서 최신순 정렬로 가져오기
        List<TipReply> list = tipReplyRepository.findByTipBoard_BnoOrderByRegTimeDesc(bno);

        // DTO 변환
        List<TipReplyDTO> dtoList = list.stream()
                .map(reply -> {
                    TipReplyDTO dto = TipReplyDTO.fromEntity(reply); // fromEntity() 사용
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponseDTO.<TipReplyDTO>withAll()
                .dtoList(dtoList)
                .total(dtoList.size())
                .build();
    }

    /**
     * 단일 댓글 조회
     */
    @Override
    @Transactional(readOnly = true)
    public TipReplyDTO read(Long rno) {
        return tipReplyRepository.findById(rno)
                .map(TipReplyDTO::fromEntity) // 수정
                .orElse(null);
    }

    /**
     * 댓글 수정
     * - 작성자 본인만 수정 가능 (Member PK 기준)
     */
    @Override
    @Transactional
    public void modify(TipReplyDTO dto) {
        TipReply reply = tipReplyRepository.findById(dto.getRno())
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!reply.getReplyer().getMno().equals(dto.getWriterId())) {
            throw new SecurityException("본인이 작성한 댓글만 수정 가능합니다.");
        }

        reply.changeText(dto.getReplyText());
        log.info("댓글 수정 완료: rno={}, 작성자Id={}", dto.getRno(), dto.getWriterId());
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void remove(Long rno, Long writerId) {
        TipReply reply = tipReplyRepository.findById(rno)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!reply.getReplyer().getMno().equals(writerId)) {
            throw new SecurityException("본인 댓글만 삭제 가능합니다.");
        }

        tipReplyRepository.delete(reply);
        log.info("댓글 삭제 완료: rno={}, 작성자Id={}", rno, writerId);
    }

    /**
     * 댓글 작성자 확인 (Controller @PreAuthorize용)
     */
    @Override
    public boolean isOwner(Long rno, String userEmail) {
        return tipReplyRepository.findById(rno)
                .map(reply -> reply.getReplyer().getEmail().equals(userEmail))
                .orElse(false);
    }
}