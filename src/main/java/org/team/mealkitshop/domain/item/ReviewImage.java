package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "review")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity @Builder
@Table(name = "review_img", indexes = {
        @Index(name = "idx_reviewimg_review", columnList = "review_id")
})
public class ReviewImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_img_id")
    @EqualsAndHashCode.Include
    private Long id;

    /** 저장 파일명 */
    @Column(nullable = false)
    private String imgName;

    /** 원본 파일명 */
    @Column(nullable = false)
    private String oriImgName;

    /** 공개 경로 (/images/...) */
    @Column(nullable = false, length = 500)
    private String imgUrl;

    /** 소속 리뷰 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
}
