package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.common.BoardBaseTimeEntity;

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
@Table(name = "tip_board")
public class TipBoard extends BaseTimeEntity {
    /**
     * ======================================
     * 💡 Tip 게시판 엔티티
     * --------------------------------------
     * - 게시글 정보
     * - 작성자, 좋아요/싫어요 수
     * - 댓글(TipReply)과 1:N 연관관계
     * - Builder 패턴 + 연관관계 편의 메서드 제공
     * ======================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;               // 게시글 번호 (PK)

    @Column(length = 30, nullable = false)
    private String title;           // 제목

    @Column(length = 1000, nullable = false)
    private String content;         // 내용

    @Column(length = 15, nullable = false)
    private String writer;          // 작성자

    private Long writerId;          // 작성자 ID

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;      // 좋아요 수

    @Builder.Default
    @Column(nullable = false)
    private int dislikeCount = 0;   // 싫어요 수

    // 인기글 상위 5개 여부 저장 (DB 컬럼 타입에 맞춰 Integer or Boolean)
    @Column(name = "top_helpful")
    private Integer topHelpful;

    // 조회수
    @Builder.Default
    private int viewCount = 0;

    @OneToMany(mappedBy = "tipBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TipBoardReaction> reactions = new ArrayList<>();

    // Tip 댓글 연관관계
    @OneToMany(mappedBy = "tipBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("regTime DESC") // 최신순 정렬
    @Builder.Default
    private List<TipReply> replies = new ArrayList<>();

    @Transient
    private boolean topLiked = false;

    // =========================
    // 연관관계 편의 메서드
    // =========================

    /**
     * 댓글 추가
     * - TipReply의 tipBoard를 자동으로 세팅
     */
    public void addReply(TipReply reply) {
        this.replies.add(reply);
        reply.setTipBoard(this);
    }

    /**
     * 댓글 제거
     * - TipReply의 tipBoard를 null로 처리
     */
    public void removeReply(TipReply reply) {
        this.replies.remove(reply);
        reply.setTipBoard(null);
    }

    // =========================
    // 좋아요/싫어요 관련 메서드
    // =========================

    /** 좋아요 1 증가 */
    public void increaseLike() {
        this.likeCount++;
    }

    /** 좋아요 1 감소 */
    public void decreaseLike() {
        if (this.likeCount > 0) this.likeCount--;
    }

    /** 싫어요 1 증가 */
    public void increaseDislike() {
        this.dislikeCount++;
    }

    /** 싫어요 1 감소 */
    public void decreaseDislike() {
        if (this.dislikeCount > 0) this.dislikeCount--;
    }

    // =========================
    // 게시글 정보 수정용 메서드
    // =========================

    /**
     * 게시글 제목/내용 수정
     */
    public void updateBoard(String title, String content) {
        this.title = title;
        this.content = content;
    }
}