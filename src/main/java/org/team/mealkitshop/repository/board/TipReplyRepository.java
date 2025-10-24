package org.team.mealkitshop.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipReply;

import java.util.List;

@Repository
public interface TipReplyRepository extends JpaRepository<TipReply, Long> {

    // 특정 TIP 게시글의 댓글 전체 조회
    List<TipReply> findByTipBoardBno(Long tipBoardId);

    // TIP 게시글 번호 기준 댓글 목록 조회 (페이징)
    Page<TipReply> findByTipBoard_Bno(Long tipBoardId, Pageable pageable);

    // 특정 TIP 게시글의 댓글 전체 삭제
    void deleteByTipBoardBno(Long tipBoardId);

    void deleteAllByTipBoard(TipBoard tipBoard);

    /** ✅ 특정 게시글 댓글 수 조회 */
    long countByTipBoardBno(Long bno);

    // Tip 게시글 댓글 최신순 조회
    List<TipReply> findByTipBoard_BnoOrderByRegTimeDesc(Long bno);

}