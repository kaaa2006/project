package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

@Entity(name = "TipBoardReply")     // 테이블 관리용 객체
@Table(name = "Tip_board_reply") // DB 테이블명
@Getter     // 게터용
@Setter
@Builder    // 세터 대신 빌더패턴 필수로 @AllArgsConstructor @NoArgsConstructor
@AllArgsConstructor // 모든 필드를 생성자 파라미터로 처리
@NoArgsConstructor  // 기본생성자용
@ToString(exclude = "tip_board")  // tipBoard 제외, 무한루프 방지
public class TipReply extends BaseTimeEntity {

    @Id // pk로 선언
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동번호생성
    private Long rno; // 댓글 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bno")
    private TipBoard tipBoard; // TipBoard FK

    private String replyText;   // 댓글 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replyer")
    private Member replyer;     // 댓글 작성자(로그인 유저 작성자)

    /** 댓글의 게시글 설정 (양방향 연관관계) */
    public void setTipBoard(TipBoard tipBoard) {
        this.tipBoard = tipBoard;
        if (!tipBoard.getReplies().contains(this)) {
            tipBoard.getReplies().add(this);
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
