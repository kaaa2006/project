// src/main/java/org/team/mealkitshop/dto/item/ReviewReplyDTO.java
package org.team.mealkitshop.dto.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 리뷰 답변 관련 DTO 모음
 * ------------------------------------------------------
 * - ReviewReplyDTO : 관리자 답변 정보를 클라이언트에 응답할 때 사용
 * - ReviewReplyDTO.Write : 클라이언트가 답변 작성/수정 시 요청 바디로 전달할 때 사용
 * 특징:
 *   • 응답과 요청을 한 파일에 통합 관리하여 가독성과 유지보수성 향상
 *   • Write 클래스는 정적 중첩 클래스로 정의 → 어디서든 import 및 사용 가능
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReplyDTO {

    // ===== 응답/조회용 필드 =====
    private Long id;          // 답변 ID (PK)
    private Long reviewId;    // 어떤 리뷰에 대한 답변인지 (FK)
    private Long adminId;     // 답변 작성자(관리자) ID
    private String adminName; // 답변 작성자(관리자) 이름
    private String content;   // 답변 본문

    // 등록/수정 시각
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regTime;    // 등록 시각
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 마지막 수정 시각

    // ===== 작성/수정 요청용 DTO =====
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Write {
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(max = 1000, message = "답변은 최대 1000자까지 가능합니다.")
        private String content;
    }
}
