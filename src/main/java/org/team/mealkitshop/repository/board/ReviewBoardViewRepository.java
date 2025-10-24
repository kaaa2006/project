package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardView;

@Repository
public interface ReviewBoardViewRepository extends JpaRepository<ReviewBoardView, Long> {
    boolean existsByBoard_BnoAndMember_Mno(Long bno, Long mno);

    @Modifying
    @Query("DELETE FROM ReviewBoardView v WHERE v.board = :board")
    void deleteAllByBoard(@Param("board") ReviewBoard board);
}
