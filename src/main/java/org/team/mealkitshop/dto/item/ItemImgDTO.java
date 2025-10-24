package org.team.mealkitshop.dto.item;

import lombok.*;
import org.modelmapper.ModelMapper;
import org.team.mealkitshop.domain.item.ItemImage;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemImgDTO {

    private Long id;

    private String imgName;   // 파일명
    private String oriImgName;// 원본명
    private String imgUrl;    // 경로

    /** 엔티티와 동일한 이름으로 통일: repimgYn */
    private Boolean repimgYn; // 대표 이미지 여부

    private static final ModelMapper modelMapper = new ModelMapper();


    /** 엔티티 -> DTO 변환 정적 팩토리 (권장) */
    public static ItemImgDTO from(ItemImage img) {
        if (img == null) return null;
        return new ItemImgDTO(
                img.getId(),
                img.getImgName(),
                img.getOriImgName(),
                img.getImgUrl(),
                // 엔티티가 primitive boolean 이면 Boolean.valueOf(img.isRepimgYn()) 로 매핑
                img.getRepimgYn() != null ? img.getRepimgYn() : Boolean.FALSE
        );
    }
}
