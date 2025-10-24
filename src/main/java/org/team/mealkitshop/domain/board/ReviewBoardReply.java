package org.team.mealkitshop.domain.board;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

@Entity(name = "ReviewBoardReply") // 테이블 관리용 객체
@Table(name = "Review_board_reply") // DB 테이블명
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "review_board")
public class ReviewBoardReply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno; // 댓글 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_bno")
    @JsonBackReference
    private ReviewBoard reviewBoard; // ReviewBoard FK

    private String replyText;   // 댓글 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Member replyer;     // 댓글 작성자(로그인 유저)

    private boolean secret;     // 비밀 댓글 여부

    /** 댓글의 게시글 설정 (양방향 연관관계) */
    public void setReviewBoard(ReviewBoard reviewBoard) {
        // 기존 board와 연결되어 있다면 제거
        if (this.reviewBoard != null && this.reviewBoard.getReplies().contains(this)) {
            this.reviewBoard.getReplies().remove(this);
        }

        this.reviewBoard = reviewBoard;

        if (reviewBoard != null && !reviewBoard.getReplies().contains(this)) {
            reviewBoard.getReplies().add(this);
        }
    }

    /** 댓글 작성자 설정 */
    public void setReplyer(Member replyer) {
        this.replyer = replyer;
    }

    /** 댓글 내용 수정 */
    public void changeText(String text) {
        this.replyText = text;
    }
}