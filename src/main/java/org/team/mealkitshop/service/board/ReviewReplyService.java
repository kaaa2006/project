package org.team.mealkitshop.service.board;

import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReviewReplyDTO;

import java.util.List;

public interface ReviewReplyService {

    // 댓글 등록 (관리자만 작성 가능)
    Long addReply(ReviewReplyDTO reviewReplyDTO);

    // 댓글 1개 조회
    ReviewReplyDTO read(Long rno);

    // 게시글별 댓글 목록 조회 (페이징)
    PageResponseDTO<ReviewReplyDTO> getListOfBoard(Long reviewBoardId, PageRequestDTO pageRequestDTO);

    // 페이징 없이 댓글 리스트만 반환 (편의 메서드)
    default List<ReviewReplyDTO> getReplysByBoard(Long reviewBoardId) {
        return getListOfBoard(reviewBoardId, new PageRequestDTO()).getDtoList();
    }

    // 댓글 수정 (관리자만 가능)
    void modify(ReviewReplyDTO replyDTO);

    // 일반 사용자는 삭제 불가, 호출 시 예외 발생
    void remove(Long rno);

    // 관리자 전용 댓글 삭제
    void removeByAdmin(Long rno);
}