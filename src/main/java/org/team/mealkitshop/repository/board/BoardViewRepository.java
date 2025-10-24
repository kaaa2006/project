package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.team.mealkitshop.domain.board.BoardView;

public interface BoardViewRepository extends JpaRepository<BoardView, Long> {
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
    boolean existsByBoardIdAndCookieId(Long boardId, String cookieId);
}
