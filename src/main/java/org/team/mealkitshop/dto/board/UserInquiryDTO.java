package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInquiryDTO {
    // 사용자용 1:1 문의 DTO
    // 사용자가 자신의 문의만 조회/작성 가능
    // 답변 내용과 상태만 표시

    private Long id;             // 문의 ID

    private String title;        // 문의 제목

    private String content;      // 문의 내용

    private String userName; // 작성자 이름

    private LocalDateTime regDate;

    private LocalDateTime modDate;

    private String answerContent;  // 관리자 답변 (있으면 표시)

    private LocalDateTime answerDate; // 답변 등록일 추가

    private String status;         // PENDING / ANSWERED

}