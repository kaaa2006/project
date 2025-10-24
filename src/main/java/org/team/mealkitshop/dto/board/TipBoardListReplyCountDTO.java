package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipBoardListReplyCountDTO {
    // Tip 게시판 리스트 DTO + 댓글 수

    private Long bno;         // 게시글 ID

    private String title;     // 게시글 제목

    private String writer;    // 게시글 작성자

    private LocalDateTime createDate; // 게시글 등록일

    private Long replyCount;       // 댓글 수

    private List<BoardImageDTO> boardImage; // 게시글 첨부 이미지 리스트

    // JPQL용 생성자 추가
    public TipBoardListReplyCountDTO(Long bno, String title, String writer, LocalDateTime createDate, Long replyCount) {
        this.bno = bno;
        this.title = title;
        this.writer = writer;
        this.createDate = createDate;
        this.replyCount = replyCount;
    }

}
