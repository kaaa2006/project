package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_Image")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "board") // 무한루프 방지
public class BoardImage {
    //                             @OneToMany처리에 순번에 맞게 정렬하기 위함
    // changeBoard()를 이용해서 Board 객체를 나중에 지정할 수 있게
    // Board 엔티티 삭제시 BoardAttachment 객체의 참조도 변경

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;            // PK, 자동 생성

    @Column(name = "uuid")
    private String uuid;       // 파일 식별자 (UUID)

    @Column(name = "file_name")
    private String fileName;   // 서버 저장 파일명

    @Column(name = "ori_file_name")
    private String oriFileName; // 원본 파일명

    @Column(name = "file_url")
    private String fileUrl; // 프론트에서 접근 가능한 URL

    @Column(name = "rep_img_yn")
    private String repImgYn; // "Y" 또는 "N"

    @Column(name = "content_type")
    private String contentType; // MIME 타입

    @Column(name = "folder_path")
    private String folderPath; // 저장된 폴더 경로 (예: "2025/09/11")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bno")
    private Board board;        // 연관 게시글

    @Column(nullable = false)
    private int ord; // 이미지 순서

    @Column(nullable = false)
    private long size; // 파일 크기

    // 경로 포함한 파일명 리턴
    public String getImagePath() {
        if(folderPath == null || folderPath.isEmpty()){
            return uuid + "_" + fileName;  // 루트로 접근
        }
        return folderPath + "/" + uuid + "_" + fileName;
    }

    // 연관관계 수정용 메서드
    public void setBoard(Board board) {
        this.board = board;
    }

}
