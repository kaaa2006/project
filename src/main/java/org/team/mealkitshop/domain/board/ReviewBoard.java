package org.team.mealkitshop.domain.board;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.common.BoardBaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review_board")
public class ReviewBoard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;               // 게시글 번호 (PK)

    @Column(length = 30, nullable = false)
    private String title;           // 제목

    @Column(length = 1000, nullable = false)
    private String content;         // 내용

    @Column(length = 15, nullable = false)
    private String writer;          // 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member writerMember;     // ✅ 로그인 이메일용 추가

    @Column(name = "secret_board")
    private boolean secretBoard;    // 비밀글 여부

    @Column(name = "secret_password")
    private String secretPassword;  // 비밀글 비밀번호

    @Builder.Default
    @Column(nullable = false)
    private int helpfulCount = 0;      // primitive int 기본값 0 (좋아요 수)

    @Builder.Default
    @Column(nullable = false)
    private int notHelpfulCount = 0;   // primitive int 기본값 0 (싫어요 수)

    // 조회수
    @Builder.Default
    private int viewCount = 0;

    /** transient: DB에는 저장 안됨, 화면 표시용 인기글 플래그 */
    @Transient
    private boolean topHelpful = false;

    // getter/setter for topHelpful
    public boolean isTopHelpful() {
        return topHelpful;
    }

    public void setTopHelpful(boolean topHelpful) {
        this.topHelpful = topHelpful;
    }

    // 댓글 연관관계
    @OneToMany(mappedBy = "reviewBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ReviewBoardReply> replies = new ArrayList<>();

    /** 도움이 됐어요/안 됐어요 카운트 증가/감소 */
    public void increaseLike() { this.helpfulCount++; }
    public void decreaseLike() { if(this.helpfulCount > 0) this.helpfulCount--; }

    public void increaseDislike() { this.notHelpfulCount++; }
    public void decreaseDislike() { if(this.notHelpfulCount > 0) this.notHelpfulCount--; }


    public boolean isSecret() {
        return this.secretBoard;
    }

}
