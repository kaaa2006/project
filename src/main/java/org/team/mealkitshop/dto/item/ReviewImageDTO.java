
package org.team.mealkitshop.dto.item;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReviewImageDTO {

    private Long id;

    // 저장 파일명/원본 파일명/공개 URL
    @NotBlank(message = "이미지 저장 파일명이 비어있습니다.")
    private String imgName;

    @NotBlank(message = "원본 파일명이 비어있습니다.")
    private String oriImgName;

    @NotBlank(message = "이미지 URL이 비어있습니다.")
    private String imgUrl;
}
