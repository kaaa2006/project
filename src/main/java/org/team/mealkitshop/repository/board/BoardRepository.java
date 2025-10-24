package org.team.mealkitshop.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.repository.board.Search.BoardSearch;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {

    // 제목 검색 + 페이징
    Page<Board> findByTitleContainingOrderByBnoDesc(String keyword, Pageable pageable);

    // JPQL 키워드 검색
    @Query("select b from Board b where b.title like concat('%',:keyword,'%')")
    Page<Board> findKeyword(String keyword, Pageable pageable);

    // 이미지 같이 로딩
    @EntityGraph(attributePaths = {"imageSet"})
    @Query("select b from Board b where b.bno = :bno")
    Optional<Board> findByIdWithImage(Long bno);

    // EVENT 글 중 과거 글
    Page<Board> findByBoardTypeAndEndDateBefore(BoardType boardType, LocalDateTime now, Pageable pageable);

    // 진행중 이벤트
    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.imageSet " +
            "WHERE b.boardType = :boardType AND (b.endDate IS NULL OR b.endDate > :now) " +
            "ORDER BY b.bno DESC")
    Page<Board> findOngoingEvents(@Param("boardType") BoardType boardType,
                                  @Param("now") LocalDateTime now,
                                  Pageable pageable);

    // 모든 글 (과거 + 진행중)
    Page<Board> findByBoardType(BoardType boardType, Pageable pageable);

}