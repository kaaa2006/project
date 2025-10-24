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
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReviewReplyDTO;
import org.team.mealkitshop.repository.board.ReviewBoardReplyRepository;
import org.team.mealkitshop.repository.board.ReviewBoardRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReviewReplyServiceImpl implements ReviewReplyService {

    private final ReviewBoardReplyRepository reviewReplyRepository; // 댓글 DB 접근
    private final ReviewBoardRepository reviewBoardRepository; // 댓글 연결 게시글 조회용
    private final MemberRepository memberRepository; // 댓글 작성자 조회용
    private final ModelMapper modelMapper; // DTO ↔ Entity 변환

    /**
     * 댓글 등록 (관리자만 가능)
     * @param reviewReplyDTO 댓글 DTO (reviewBoardId 포함)
     * @return 등록된 댓글 RNO
     */
    @Override
    @Transactional
    public Long addReply(ReviewReplyDTO reviewReplyDTO) {
        log.info("ReviewReply 등록 전 DTO : {}", reviewReplyDTO);

        // 1. 게시글 엔티티 조회 (DB에서 가져와야 transient 오류 방지)
        ReviewBoard board = reviewBoardRepository.findById(reviewReplyDTO.getReviewBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 2. 댓글 작성자 조회
        Member replyer = memberRepository.findByMemberName(reviewReplyDTO.getReplyer())
                .orElseThrow(() -> new IllegalArgumentException("작성자가 존재하지 않습니다."));

        // 3. 댓글 엔티티 생성
        ReviewBoardReply reply = ReviewBoardReply.builder()
                .reviewBoard(board) // DB에서 가져온 게시글 연결
                .replyer(replyer)   // 작성자 연결
                .replyText(reviewReplyDTO.getReplyText())
                .secret(reviewReplyDTO.isSecret()) // 비밀댓글 설정 가능
                .build();

        // 4. 댓글 저장
        reviewReplyRepository.save(reply);

        return reply.getRno();
    }

    /**
     * 단일 댓글 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ReviewReplyDTO read(Long rno) {
        ReviewBoardReply reply = reviewReplyRepository.findById(rno)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        return modelMapper.map(reply, ReviewReplyDTO.class);
    }

    /**
     * 게시글별 댓글 목록 조회 (페이징)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewReplyDTO> getListOfBoard(Long reviewBoardId, PageRequestDTO pageRequestDTO) {

        // 최신 댓글이 위로 오도록 Repository 메서드 사용
        List<ReviewBoardReply> list = reviewReplyRepository.findByReviewBoard_BnoOrderByRegTimeDesc(reviewBoardId);

        List<ReviewReplyDTO> dtoList = list.stream()
                .map(reply -> modelMapper.map(reply, ReviewReplyDTO.class))
                .collect(Collectors.toList());

        // 페이징이 필요하면 subList 처리 가능
        int start = (pageRequestDTO.getPage() - 1) * pageRequestDTO.getSize();
        int end = Math.min(start + pageRequestDTO.getSize(), dtoList.size());
        List<ReviewReplyDTO> pagedList = dtoList.subList(start, end);

        return PageResponseDTO.<ReviewReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(pagedList)
                .total(dtoList.size())
                .build();
    }

    /**
     * 댓글 수정 (관리자만 가능)
     */
    @Override
    @Transactional
    public void modify(ReviewReplyDTO reviewReplyDTO) {
        ReviewBoardReply reply = reviewReplyRepository.findById(reviewReplyDTO.getRno())
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        reply.changeText(reviewReplyDTO.getReplyText());
        reviewReplyRepository.save(reply);
    }

    /**
     * 일반 사용자는 삭제 불가
     */
    @Override
    public void remove(Long rno) {
        throw new UnsupportedOperationException("리뷰 게시판 댓글은 사용자가 삭제할 수 없습니다.");
    }

    /**
     * 관리자 전용 삭제
     */
    @Override
    @Transactional
    public void removeByAdmin(Long rno) {
        reviewReplyRepository.deleteById(rno);
    }
}