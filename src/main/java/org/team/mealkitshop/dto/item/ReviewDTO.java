package org.team.mealkitshop.dto.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReviewDTO {

    // 식별자
    private Long id;

    // 연관키
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;
    private String itemName;
    private String itemThumbUrl;

    @NotNull(message = "작성자 회원번호(mno)는 필수입니다.")
    private Long writerMno;

    // 표시용
    private String writerName;

    // 본문/평점
    @NotBlank(message = "리뷰 내용을 입력해주세요.")
    private String content;

    @Min(1)
    @Max(5)
    private Integer rating;

    // 이미지들(없으면 빈 리스트)
    @Builder.Default
    private List<ReviewImageDTO> reviewImages = new ArrayList<>();

    // 생성/수정 시각(읽기전용)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 관리자 답변(없으면 null) */
    private ReviewReplyDTO reply; // null 허용
}
