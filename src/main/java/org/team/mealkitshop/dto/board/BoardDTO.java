package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.common.BoardBaseTimeEntity;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardImage;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper=false)
public class BoardDTO extends BoardBaseTimeEntity {

    private Long bno;                   // 게시글 번호

    @NotBlank(message = "제목은 필수 입력입니다.")
    @Size(min = 5, max = 30, message = "제목은 최소 5자 이상이어야 합니다.")
    private String title;               // 제목

    @NotBlank(message = "내용은 필수 입력입니다.")
    private String content;             // 내용

    private String writer;              // 작성자

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regTime;      // 등록일

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;      // 수정일

    @NotNull(message = "게시판 유형을 선택해주세요.")
    private BoardType boardType;

    // 이벤트 시작/종료일
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean expired; // true면 종료, false면 진행중

    // 진행중 여부
    @Builder.Default
    private Boolean active = true; // 기본값 진행중

    public Boolean getExpired() {
        if (endDate == null) return false;
        return LocalDate.now().isAfter(endDate);
    }


    // 조회수
    @Builder.Default
    private int viewCount = 0;

    @Builder.Default
    private List<String> fileNames = new ArrayList<>(); // 첨부파일 목록, Builder 기본값 지정

    // ★ 추가: 업로드용 MultipartFile
    @JsonIgnore
    private List<MultipartFile> files = new ArrayList<>();

    private boolean deleted = false; // 기본값 false

    public static BoardDTO fromEntity(Board board) {
        BoardDTO dto = new BoardDTO();
        dto.setBno(board.getBno());
        dto.setTitle(board.getTitle());
        dto.setContent(board.getContent());
        dto.setWriter(board.getWriter());
        dto.setBoardType(board.getBoardType());
        dto.setRegTime(board.getRegTime());
        dto.setViewCount(board.getViewCount());

        // LocalDateTime → LocalDate 변환
        if (board.getStartDate() != null) dto.setStartDate(board.getStartDate().toLocalDate());
        if (board.getEndDate() != null) dto.setEndDate(board.getEndDate().toLocalDate());

        dto.setActive(board.getBoardType() == BoardType.EVENT
                ? board.getEndDate() == null || board.getEndDate().isAfter(LocalDateTime.now())
                : null);

        LocalDateTime update = board.getUpdateTime();
        dto.setUpdateTime(update != null && update.equals(board.getRegTime()) ? null : update);

        dto.setDeleted(board.isDeleted());

        if (board.getImageSet() != null) {
            dto.setFileNames(
                    board.getImageSet().stream()
                            .map(img -> "/uploads/" + img.getFileName()) // UUID_원본파일명 형태 그대로 사용
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}