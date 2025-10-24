package org.team.mealkitshop.service.board;

import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.TipBoardDTO;
import org.team.mealkitshop.dto.board.TipReplyDTO;

import java.util.List;

/**
 * ======================================
 * 💡 팁 게시판 댓글 서비스 인터페이스
 * --------------------------------------
 * - 댓글 등록 / 조회 / 수정 / 삭제 기능 제공
 * - 비즈니스 로직은 구현체에서 처리
 * ======================================
 */
public interface TipReplyService {

    // 댓글 등록 (로그인된 사용자 이메일 필요 없음, Controller에서 Rq로 처리)
    Long addReply(TipReplyDTO dto);

    // 특정 게시글의 댓글 목록 (페이징 처리 포함)
    PageResponseDTO<TipReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO);

    // 단일 댓글 조회
    TipReplyDTO read(Long rno);

    // 댓글 수정 (작성자 본인만 가능)
    void modify(TipReplyDTO dto);

    // 댓글 삭제 (작성자 본인만 가능)
    void remove(Long rno, Long writerId);

    // @PreAuthorize용: 작성자 확인
    boolean isOwner(Long rno, String userEmail);

}