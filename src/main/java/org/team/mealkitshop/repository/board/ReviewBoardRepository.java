package org.team.mealkitshop.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.member.Member;

import java.util.List;
import java.util.Optional;

/**
 * ReviewBoard 전용 JPA Repository
 * - ReviewBoard 엔티티 전용
 * - 후기 목록 조회, 작성자 조회, 도움 됨 기준 상위 조회
 */
@Repository
public interface ReviewBoardRepository extends JpaRepository<ReviewBoard, Long> {

    /** 비밀글이 아닌 후기만 조회 */
    List<ReviewBoard> findBySecretBoardFalse();

    /** 특정 작성자 기준 후기 조회 */
    List<ReviewBoard> findByWriter(String writer);

    /** 도움 됐어요 기준 상위 5개 후기 게시글 조회 */
    List<ReviewBoard> findTop5ByOrderByHelpfulCountDesc();

    @Query("SELECT b FROM ReviewBoard b " +
            "LEFT JOIN FETCH b.replies r " +
            "LEFT JOIN FETCH r.replyer " +
            "WHERE b.bno = :bno")
    Optional<ReviewBoard> findByIdWithReplies(@Param("bno") Long bno);

    @Query("SELECT r FROM ReviewBoard r ORDER BY r.helpfulCount DESC")
    List<ReviewBoard> findTopHelpful(Pageable pageable);

    // 최신글 Top 50 조회 (모든 글 포함)
    List<ReviewBoard> findTop50ByOrderByRegTimeDesc();

    /** 페이징 전체 조회 (비밀글 포함) */
    Page<ReviewBoard> findAll(Pageable pageable);

    /** 비밀글 제외 페이징 조회 */
    Page<ReviewBoard> findBySecretBoardFalse(Pageable pageable);

    @Query("SELECT b FROM ReviewBoard b " +
            "LEFT JOIN FETCH b.writerMember " +
            "LEFT JOIN FETCH b.replies r " +
            "LEFT JOIN FETCH r.replyer " +
            "ORDER BY b.regTime DESC")
    List<ReviewBoard> findTop50WithWriterAndReplies();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReviewBoard b SET b.helpfulCount = b.helpfulCount + :delta WHERE b.bno = :bno")
    int updateHelpfulCount(@Param("bno") Long bno, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReviewBoard b SET b.notHelpfulCount = b.notHelpfulCount + :delta WHERE b.bno = :bno")
    int updateNotHelpfulCount(@Param("bno") Long bno, @Param("delta") int delta);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
            "FROM ReviewBoardView v WHERE v.board.bno = :bno AND v.member.mno = :mno")
    boolean existsByBnoAndViewerId(@Param("bno") Long bno, @Param("mno") Long mno);

    @Modifying
    @Query("INSERT INTO ReviewBoardView(board, member) VALUES(:board, :member)")
    void insertViewRecord(@Param("board") ReviewBoard board, @Param("member") Member member);
}
