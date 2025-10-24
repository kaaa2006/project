package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.domain.member.Member;

@Entity
@Table(name = "tip_board_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipBoardView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 게시글을 조회했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_board_id")
    private TipBoard tipBoard;

    // 누가 조회했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
