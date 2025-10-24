package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipBoardView;

@Repository
public interface TipBoardViewRepository extends JpaRepository<TipBoardView, Long> {

    boolean existsByTipBoard_BnoAndMember_Mno(Long bno, Long mno);

    @Modifying
    @Query("DELETE FROM TipBoardView v WHERE v.tipBoard = :tipBoard")
    void deleteAllByTipBoard(@Param("tipBoard") TipBoard tipBoard);
}
