package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_board_image")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "reviewBoard") // 무한루프 방지
public class ReviewBoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_board_id")
    private ReviewBoard reviewBoard;

    public void setReviewBoard(ReviewBoard reviewBoard) {
        this.reviewBoard = reviewBoard;
    }
}