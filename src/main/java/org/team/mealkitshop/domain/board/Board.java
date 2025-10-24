package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.common.BoardBaseTimeEntity;
import org.team.mealkitshop.common.BoardType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 공용 게시글 엔티티 최소화
 * - TIP/REVIEW 별도로 관리하므로 상속 구조 제거
 * - 공용 이미지 관리만 포함
 */
@Entity
@Table(name = "board")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(exclude = "imageSet") // 이미지 연관 무한루프 방지
public class Board extends BoardBaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;               // 게시글 번호 (PK)

    @Column(length = 30, nullable = false)
    private String title;           // 제목

    @Column(length = 1000, nullable = false)
    private String content;         // 내용

    @Column(length = 15, nullable = false)
    private String writer;          // 작성자

    @NotNull(message = "게시판 유형을 선택해주세요.")
    @Enumerated(EnumType.ORDINAL)
    private BoardType boardType;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0; // 기본값 0

    public void increaseViewCount() {
        this.viewCount += 1;
    }

    private LocalDateTime startDate; // 이벤트 시작일
    private LocalDateTime endDate;   // 이벤트 종료일
    private Boolean active;          // 진행중 여부

    /** 이미지 연관관계 */
    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 20) // N+1 문제 방지용
    private Set<BoardImage> imageSet = new HashSet<>();

    /** 게시글 제목/내용 수정 */
    public void change(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /**
     * 이미지 추가
     * - ord 제거, Set에 순서 상관없이 추가
     * - builder로 BoardImage 생성 후 board 연관 설정
     */
    public void addImage(String fileName) {
        BoardImage boardImage = BoardImage.builder()
                .fileName(fileName)
                .board(this)
                .build();
        imageSet.add(boardImage);
    }

    /** 이미지 모두 삭제 */
    public void clearImage() {
        imageSet.forEach(img -> img.setBoard(null));
        imageSet.clear();
    }

    /** 특정 이미지 제거 (파일명 기준) */
    public void removeImage(String fileName) {
        imageSet.removeIf(img -> img.getFileName().equals(fileName));
    }

    @Column(name = "deleted")
    private boolean deleted = false; // 기본값 false

    public void delete() {
        this.deleted = true;
    }
}