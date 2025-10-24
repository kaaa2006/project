package org.team.mealkitshop.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.BoardImage;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    // 특정 게시글의 이미지 모두 조회
    List<BoardImage> findByBoard_Bno(Long bno);

    // 게시글 이미지 삭제
    void deleteByBoard_Bno(Long bno);

    Optional<BoardImage> findByFileName(String fileName);
}