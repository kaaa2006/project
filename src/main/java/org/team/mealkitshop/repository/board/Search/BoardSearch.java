package org.team.mealkitshop.repository.board.Search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.dto.board.BoardListAllDTO;
import org.team.mealkitshop.dto.board.TipBoardListReplyCountDTO;

public interface BoardSearch {

    Page<Board> search1(Pageable pageable);

    Page<Board> searchAll(String[] types, String keyword, Pageable pageable);

    Page<TipBoardListReplyCountDTO> searchWithReplyCount(String[] types, String keyword, Pageable pageable);

    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable);

}
