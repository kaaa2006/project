package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tip_board_image")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "tipBoard") // 무한루프 방지
public class TipBoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // 이미지 PK

    private String fileName;        // 서버 저장 파일명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_board_id")
    private TipBoard tipBoard;      // TIP 게시글과 연관

    public void setTipBoard(TipBoard tipBoard) {
        this.tipBoard = tipBoard;
    }
}