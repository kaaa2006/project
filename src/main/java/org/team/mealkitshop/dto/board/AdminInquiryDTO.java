package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.AnswerStatus;
import org.team.mealkitshop.domain.board.InquiryBoard;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInquiryDTO {
    // 관리자용 1:1 문의 DTO
    // 모든 문의 조회 가능
    // 답변 작성/수정 가능

    private Long id;               // 문의 ID

    private String title;          // 문의 제목

    private String content;        // 문의 내용

    private String userEmail; // 작성자 이메일

    private String userName;       // 작성자 이름

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime regDate; // 문의 받은 날짜

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime modDate; // 문의 수정 날짜

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime answerDate; // 답변 작성 날짜

    private String answerContent;  // 관리자 답변 작성 가능

    private String status;         // PENDING / ANSWERED

    // ✅ Entity → DTO 변환 메서드
    public static AdminInquiryDTO fromEntity(InquiryBoard inquiryBoard) {

        AnswerStatus answerStatus = inquiryBoard.getStatus() != null ? inquiryBoard.getStatus() : AnswerStatus.PENDING;

        return AdminInquiryDTO.builder()
                .id(inquiryBoard.getId())
                .title(inquiryBoard.getTitle())
                .content(inquiryBoard.getContent())
                .userEmail(inquiryBoard.getWriter().getEmail())
                .userName(inquiryBoard.getWriter().getMemberName())
                .regDate(inquiryBoard.getRegTime())
                .modDate(inquiryBoard.getUpdateTime())
                .answerDate(inquiryBoard.getAnswer() != null
                        ? inquiryBoard.getAnswer().getRegTime()
                        : null)
                .answerContent(inquiryBoard.getAnswer() != null ? inquiryBoard.getAnswer().getContent() : null)
                .status(answerStatus.getDisplayName()) // 여기서 한글 표시
                .build();
    }
}