package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.domain.member.Member;

/* 추후 해당 기능 미사용 시 제거 예정 */
@Entity
@Table(
        name = "item_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id","item_id"})
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Builder
    private ItemLike(Member member, Item item) {
        this.member = member;
        this.item = item;
    }
}
