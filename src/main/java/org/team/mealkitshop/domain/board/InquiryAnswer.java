package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryAnswer extends BaseTimeEntity {
    // 1:1 문의 답변 엔티티
    // 관리자 전용 작성
    // 작성 및 수정 일자 추적 가능

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @OneToOne
    @JoinColumn(name = "inquiry_id", nullable = false)
    private InquiryBoard inquiryBoard; // 어떤 문의에 대한 답변인지

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;    // 답변 작성 관리자

    @Column(nullable = false, length = 1000)
    private String content;  // 답변 내용

    /*@Column(nullable = false)
    private LocalDateTime regDate;  // 답변 작성일

    @Column(nullable = false)
    private LocalDateTime modDate;  // 답변 마지막 수정일 (관리자가 수정 시)*/
}