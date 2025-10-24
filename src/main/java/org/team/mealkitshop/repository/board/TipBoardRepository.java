package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.dto.board.TipBoardListReplyCountDTO;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TipBoardRepository extends JpaRepository<TipBoard, Long> {

    // 최근 일정 기간 내 TIP 게시글 중 좋아요 상위 5개 조회
    List<TipBoard> findTop5ByRegTimeAfterOrderByLikeCountDesc(LocalDateTime after);

    // 특정 TIP 게시글 제외하고 최신순 조회
    List<TipBoard> findByBnoNotInOrderByRegTimeDesc(List<Long> excludedBnos);

    // TIP 게시글 + 댓글 수 조회 (DTO 반환)
    // 주의: TipBoard 엔티티 필드명과 TipReply 엔티티 필드명을 정확히 맞춰야 함
    @Query("""
        SELECT new org.team.mealkitshop.dto.board.TipBoardListReplyCountDTO(
            b.bno, b.title, b.writer, b.regTime, COUNT(r.rno)
        )
        FROM TipBoard b
        LEFT JOIN b.replies r ON r.tipBoard = b
        GROUP BY b.bno
        ORDER BY b.regTime DESC
        """)
    List<TipBoardListReplyCountDTO> getBoardListWithReplyCount();

    List<TipBoard> findAllByOrderByRegTimeDesc();

}
