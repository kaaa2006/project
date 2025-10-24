package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BoardReactionType;

/**
 * ReviewBoard 전용 좋아요/도움 됐어요 엔티티
 * - 하나의 게시글에 사용자 하나의 반응만 존재하도록 unique 제약
 */
@Entity
@Table(name = "review_board_reaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_board_id", "user_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "reviewBoard") // 연관 엔티티 제외 -> 무한루프 방지
public class ReviewBoardReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_board_id", nullable = false)
    private ReviewBoard reviewBoard; // 게시글 연관

    @Column(name = "user_id", nullable = false)
    private String userId; // 사용자 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardReactionType reaction; // HELPFUL / NOT_HELPFUL

    /** 반응 변경 */
    public void changeReaction(BoardReactionType reaction){
        this.reaction = reaction;
    }

}
