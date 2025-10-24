package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.domain.member.Member;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "review",
        indexes = {@Index(name = "ix_review_item_id_id", columnList = "item_id, id"),
        // 아이템 상세 페이지에서 최신순 페이지닝 가속
                @Index(name = "ix_review_mno", columnList = "mno")})
        // 마이페이지 등 회원별 조회가 있으면 유용


@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"member", "item","images"})
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mno", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String content;

    @Min(1) @Max(5)
    private int rating;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();


    // 편의 메서드(필요시)
    public void addImage(ReviewImage image) {
        this.images.add(image);
        image.setReview(this); // 소유측 동기화
    }

    public void removeImage(ReviewImage image) {
        this.images.remove(image);
        image.setReview(null);
    }

    public void changeContent(String content) {
        String trimmed = (content == null) ? "" : content.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("내용은 비어있을 수 없습니다.");
            }
            this.content = trimmed;
        }

    public void changeRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
        this.rating = rating;
    }

}
