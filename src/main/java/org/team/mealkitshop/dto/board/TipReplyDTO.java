package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.domain.board.TipReply;
import org.team.mealkitshop.domain.member.Member;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipReplyDTO {
    // 📌 Tip 게시판 댓글 DTO - REST용

    private Long rno;   // 댓글 ID (PK)

    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long tipBoardId;  // 댓글이 달릴 게시글 번호 (FK)

    @NotEmpty(message = "댓글 내용은 필수입니다.")
    private String replyText; // 댓글 내용

    private Member replyer;     // 댓글 작성자(로그인 유저 작성자)

    // 작성자 정보는 입력받지 않고 서버에서 자동 세팅
    private Long writerId;       // 작성자 ID (서버에서 채움)
    private String writerName;   // 작성자 이름 (조회 시 채움)

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate; // 등록일

    @JsonIgnore
    private LocalDateTime modDate; // 수정일

    public static TipReplyDTO fromEntity(TipReply reply) {
        TipReplyDTO dto = new TipReplyDTO();
        dto.setRno(reply.getRno());
        dto.setTipBoardId(reply.getTipBoard().getBno());
        dto.setReplyText(reply.getReplyText());
        dto.setRegDate(reply.getRegTime());
        dto.setModDate(reply.getUpdateTime());

        if (reply.getReplyer() != null) {
            dto.setWriterId(reply.getReplyer().getMno());
            dto.setWriterName(reply.getReplyer().getMemberName());
        } else {
            dto.setWriterName("익명");
        }
        return dto;
    }
}
