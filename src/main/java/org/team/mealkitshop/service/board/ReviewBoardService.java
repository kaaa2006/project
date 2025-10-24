package org.team.mealkitshop.service.board;

import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.ReviewBoardDTO;

import java.util.List;

/**
 * 리뷰 게시판 전용 서비스 인터페이스
 * - 회원 글쓰기 가능, 관리자 댓글만 가능
 * - 도움 됐어요 기준 조회
 */
public interface ReviewBoardService {

    // 게시글 등록
    Long register(ReviewBoardDTO dto);

    // 단건 조회
    ReviewBoardDTO readOne(Long bno, Long MemberId);

    // 댓글 포함 게시글 조회
    ReviewBoard readBoardWithReplies(Long bno);

    // 게시글 수정
    void modify(ReviewBoardDTO dto);

    // 게시글 삭제
    void remove(Long bno, Member member);

    // 도움 됐어요 기준 상위 리뷰 조회
    List<ReviewBoard> getTopHelpfulReviews(int topCount);

    // ✅ 게시판 리스트 조회 (상위 인기글 포함)
    List<ReviewBoard> getReviewBoardList();

    // ==========================
    // 새로 추가: 댓글 등록
    // ==========================
    ReviewBoardReply registerReply(Long bno, Member member, String replyText);

    /**
     * 로그인 회원일 경우 게시글 조회 시 조회수 증가
     */
    void incrementViewCountForMember(ReviewBoard board, Long memberId);

    /**
     * 비밀번호 확인
     * @param board 조회할 게시글
     * @param password 사용자가 입력한 비밀번호
     * @param member 확인할 회원 (null 가능)
     * @return 비밀번호가 맞거나 관리자인 경우 true
     */
    boolean checkPassword(ReviewBoard board, String password, Member member);
}