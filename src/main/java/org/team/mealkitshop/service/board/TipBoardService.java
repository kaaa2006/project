package org.team.mealkitshop.service.board;

import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.TipBoardDTO;

import java.util.List;

/**
 * TIP 게시판 CRUD 전용 서비스 인터페이스
 * - 로그인 회원 기준으로 작성/수정/삭제
 * - TIP 게시판 전용 기능 제공
 */
public interface TipBoardService {

    // 게시글 등록 (로그인 회원 기준)
    Long register(TipBoardDTO dto, Member member);

    // 단건 조회 (모든 회원 가능)
    TipBoardDTO readOne(Long bno, String userId);

    // 게시글 수정 (본인 글만 가능)
    void modify(TipBoardDTO dto, Member member);

    // 게시글 삭제 (본인 글만 가능)
    void remove(Long bno, Member member);

    // 최신 TIP 조회
    List<TipBoard> getRecentTips();

    // 상위 좋아요 TIP 조회
    List<TipBoard> getTopLikedTips(int topCount);

    // 좋아요/싫어요 토글
    void toggleReaction(Long bno, String userId, BoardReactionType type);

    // 🔹 새로 추가: 회원별 조회수 증가
    void incrementViewCountForMember(TipBoard board, Long memberId);
}