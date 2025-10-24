package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.AnswerStatus;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

import java.time.LocalDateTime;

@Entity
@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryBoard extends BaseTimeEntity {
    // 1:1 문의 게시판 엔티티
    // 회원이 작성
    // 답변은 InquiryAnswer로 관리
    // 답변 상태 및 작성/수정 일자 추적 가능

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @ManyToOne
    @JoinColumn(name = "user_mno", nullable = false)
    private Member writer;    // 문의 작성자 (Member 엔티티와 다대일 관계)

    @Column(nullable = false, length = 100)
    private String title;     // 문의 제목

    @Column(nullable = false, length = 1000)
    private String content;   // 문의 내용

    @Column(nullable = false)
    private LocalDateTime regTime;  // 문의 작성일

    @Column(nullable = false)
    private LocalDateTime updateTime;  // 문의 마지막 수정일

    // 답변과 1:1 매핑
    // cascade.ALL: 문의 삭제 시 답변도 같이 삭제
    // fetch.LAZY: 답변이 필요할 때만 조회
    @OneToOne(mappedBy = "inquiryBoard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private InquiryAnswer answer;  // 문의 답변 (관리자 전용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerStatus status = AnswerStatus.PENDING; // 답변 상태: PENDING / COMPLETED 등

    /*@Column(nullable = false)
    private LocalDateTime regDate;  // 문의 작성일

    @Column(nullable = false)
    private LocalDateTime modDate;  // 문의 마지막 수정일 (회원이 수정 시)*/
}