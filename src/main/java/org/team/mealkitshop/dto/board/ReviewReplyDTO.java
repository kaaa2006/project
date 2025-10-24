package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.domain.board.ReviewBoardReply;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewReplyDTO {
    // 📌 Review 게시판 댓글 DTO - REST용
    // - 입력 시 필요한 필드와 조회용 필드를 분리
    // - 작성자 정보는 서버에서 자동 채움

    private Long rno;   // 댓글 ID (PK)


    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long reviewBoardId;   // 댓글이 달릴 게시글 번호 (FK)

    @NotEmpty(message = "댓글 내용은 필수입니다.")
    private String replyText; // 댓글 내용

    // 작성자 이름은 서버에서 자동 채움 (예: "관리자")
    private String replyer;       // 화면에 보여줄 이름

    // 추가: 이 댓글 작성자가 관리자면 true
    @Builder.Default
    private boolean admin = false;

    @JsonIgnore
    private String replyerEmail;  // DB 저장용, 화면에는 안 보여줌

    private boolean secret;  // 게시글이 비밀글이면 true

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate; // 등록일

    @JsonIgnore
    private LocalDateTime modDate; // 수정일

    // 🔹 엔티티 → DTO 변환 메서드 추가
    public static ReviewReplyDTO fromEntity(ReviewBoardReply reply) {
        String replyerName = "익명";
        String replyerEmail = null;
        boolean isAdmin = false;

        if (reply.getReplyer() != null) {
            try {
                var m = reply.getReplyer();
                replyerName = m.getMemberName();
                replyerEmail = m.getEmail();
                // ✅ 단일 필드로 관리자 판별
                isAdmin = m.getRole() == Role.ADMIN;
            } catch (Exception ignored) {}
        }

        return ReviewReplyDTO.builder()
                .rno(reply.getRno())
                .reviewBoardId(reply.getReviewBoard() != null ? reply.getReviewBoard().getBno() : null)
                .replyText(reply.getReplyText())
                .replyer(replyerName)
                .replyerEmail(replyerEmail)
                .secret(reply.isSecret())
                .regDate(reply.getRegTime())
                .modDate(reply.getUpdateTime())
                .admin(isAdmin) // ✅ 뱃지용 플래그 세팅
                .build();
    }

}
