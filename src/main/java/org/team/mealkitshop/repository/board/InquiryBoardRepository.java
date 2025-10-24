package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.InquiryBoard;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryBoardRepository extends JpaRepository<InquiryBoard, Long> {

    // 로그인한 사용자의 문의만 조회 (writer, answer 미리 fetch)
    @Query("SELECT b FROM InquiryBoard b LEFT JOIN FETCH b.writer LEFT JOIN FETCH b.answer WHERE b.writer.mno = :userMno")
    List<InquiryBoard> findByWriterMnoWithAnswer(@Param("userMno") Long userMno);

    // 특정 사용자의 특정 문의 조회 (writer, answer 미리 fetch)
    @Query("SELECT b FROM InquiryBoard b LEFT JOIN FETCH b.writer LEFT JOIN FETCH b.answer WHERE b.id = :inquiryId AND b.writer.mno = :userMno")
    Optional<InquiryBoard> findByIdAndWriterMnoWithAnswer(@Param("inquiryId") Long inquiryId, @Param("userMno") Long userMno);

    // 관리자용 모든 문의 조회 (writer, answer 미리 fetch)
    @Query("SELECT b FROM InquiryBoard b LEFT JOIN FETCH b.writer LEFT JOIN FETCH b.answer")
    List<InquiryBoard> findAllWithWriterAndAnswer();

    // 관리자용 특정 문의 조회
    @Query("SELECT b FROM InquiryBoard b LEFT JOIN FETCH b.writer LEFT JOIN FETCH b.answer WHERE b.id = :id")
    Optional<InquiryBoard> findByIdWithWriterAndAnswer(@Param("id") Long id);
}
