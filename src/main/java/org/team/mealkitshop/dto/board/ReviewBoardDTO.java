package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.ReviewBoard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REVIEW 게시판 전용 DTO
 * - BoardDTO 상속 제거, 독립적 사용
 * - 비밀글, 도움이 됐어요/안 됐어요 카운트 포함
 * - ModelMapper 사용 가능
 */
@Data
@Builder
@NoArgsConstructor // ModelMapper용 기본 생성자
@AllArgsConstructor
public class ReviewBoardDTO {

    private Long bno;                   // 게시글 번호

    @NotBlank(message = "제목은 필수 입력입니다.")
    @Size(min = 5, max = 30, message = "제목은 최소 5자 이상이어야 합니다.")
    private String title;               // 제목

    @NotBlank(message = "내용은 필수 입력입니다.")
    private String content;             // 내용

    private String writer;              // 작성자

    private String writerEmail;   // ✅ 로그인 이메일용 추가

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate;      // 등록일

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modDate;      // 수정일

    // ✅ 인기글 상위 5개 여부 표시용
    private boolean topHelpful;

    // 비밀글 여부
    @Builder.Default
    private boolean secretBoard = false;

    // 관리자나 작성자면 true: 비밀번호 없이 열람 가능
    @Builder.Default
    private boolean canViewSecret = false;

    // 비밀글 비밀번호
    private String secretPassword;

    // '도움 됐어요' 카운트
    @Builder.Default
    private int helpfulCount = 0;

    // '도움 안 됐어요' 카운트
    @Builder.Default
    private int notHelpfulCount = 0;

    // 조회수
    @Builder.Default
    private int viewCount = 0;

    // 로그인 사용자가 눌렀던 반응 상태 저장
    // "helpful", "notHelpful", null
    private String userReaction;

    // ✅ 댓글 리스트 추가
    @Builder.Default
    private List<ReviewReplyDTO> replies = new ArrayList<>();

    private boolean isAlreadyAddHelpful;     // 로그인 사용자가 이미 '도움 됐어요' 눌렀는지
    private boolean isAlreadyAddNotHelpful;  // 로그인 사용자가 이미 '도움 안 됐어요' 눌렀는지

    // 게시글이 원래 비밀글인지 체크용
    @JsonIgnore
    private boolean wasSecretBoard;

    // ==============================
    // ★ 수정용 필드 ★
    // ==============================

    private String currentPassword; // 현재 비밀번호 입력용

    // 수정 시 새 비밀번호 입력용
    private String newPassword;

    // ✅ 비밀글이면 비밀번호가 4자 이상인지 확인
    @AssertTrue(message = "비밀글 설정 시 비밀번호는 최소 4자 이상이어야 합니다.")
    public boolean isSecretPasswordValid() {
        if (!secretBoard) return true;

        // 등록(wasSecretBoard=false): 보통 secretPassword를 사용
        // 수정: 새 비번을 넣으면 newPassword, 아니면 secretPassword 유지
        String candidate = (newPassword != null && !newPassword.isBlank())
                ? newPassword
                : secretPassword;

        return candidate != null && candidate.trim().length() >= 4;
    }


    // 도움이 됐어요 DTO 생성
    public static ReviewBoardReactionDTO helpful(Long boardId, String userId) {
        return ReviewBoardReactionDTO.builder()
                .reviewBoardId(boardId)
                .userId(userId)
                .reactionType(BoardReactionType.HELPFUL)
                .build();
    }

    // 도움이 안 됐어요 DTO 생성
    public static ReviewBoardReactionDTO notHelpful(Long boardId, String userId) {
        return ReviewBoardReactionDTO.builder()
                .reviewBoardId(boardId)
                .userId(userId)
                .reactionType(BoardReactionType.NOT_HELPFUL)
                .build();
    }

    public ReviewBoardDTO(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public ReviewBoard toEntity(String writer) {
        return ReviewBoard.builder()
                .title(title)
                .content(content)
                .writer(writer)
                .build();
    }

    public static ReviewBoardDTO fromEntity(ReviewBoard board) {
        String writerEmail = null;
        String writerName = board.getWriter();

        if (board.getWriterMember() != null) {
            try {
                writerName = board.getWriterMember().getMemberName();
                writerEmail = board.getWriterMember().getEmail();
            } catch (Exception e) {
                // Lazy 로딩 실패 시 기본값 유지
            }
        }

        List<ReviewReplyDTO> replyDTOs = new ArrayList<>();
        try {
            replyDTOs = board.getReplies().stream()
                    .map(r -> ReviewReplyDTO.fromEntity(r))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Lazy 실패 시 빈 리스트
        }

        return ReviewBoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(writerName)
                .writerEmail(writerEmail)
                .regDate(board.getRegTime())
                .modDate(board.getUpdateTime())
                .viewCount(board.getViewCount())
                .helpfulCount(board.getHelpfulCount())
                .notHelpfulCount(board.getNotHelpfulCount())
                .secretBoard(board.isSecretBoard())
                .secretPassword(board.getSecretPassword())
                .replies(replyDTOs)
                .build();
    }



}
