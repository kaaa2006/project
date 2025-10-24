package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

@Entity
@Table(
        name = "review_reply",
        uniqueConstraints = { @UniqueConstraint(columnNames = "review_id") } // 리뷰당 답변 1개 보장 (유니크 제약)
)
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"review", "admin"})
public class ReviewReply extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;   // 답변 PK

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Review 삭제 시 DB 레벨로 Reply 자동 삭제
    @JoinColumn(name = "review_id", nullable = false, unique = true) // 컬럼 자체에도 unique 명시
    private Review review;

    // 답변 작성자(관리자) — member.mno 참조
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    @Column(nullable = false, length = 1000)
    private String content; // 답변 내용
}
