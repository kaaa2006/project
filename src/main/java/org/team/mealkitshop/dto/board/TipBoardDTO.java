package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.TipBoard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TIP 게시판 전용 DTO
 * - BoardDTO 상속 제거, 독립적 사용
 * - 좋아요/싫어요 카운트 포함
 * - ModelMapper 사용 가능
 */
@Data
@Builder
@NoArgsConstructor // ModelMapper용 기본 생성자
@AllArgsConstructor
public class TipBoardDTO {

    private Long bno;                   // 게시글 번호

    @NotBlank(message = "제목은 필수 입력입니다.")
    @Size(min = 5, max = 30, message = "제목은 최소 5자 이상이어야 합니다.")
    private String title;               // 제목

    @NotBlank(message = "내용은 필수 입력입니다.")
    private String content;             // 내용

    private String writer;              // 작성자

    private Long writerId; // 게시글 작성자 Member ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate;      // 등록일

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modDate;      // 수정일

    // 조회수
    @Builder.Default
    private int viewCount = 0;

    // ✅ 인기글 상위 5개 여부 표시용
    private boolean topHelpful;

    // ✅ 댓글 리스트 추가
    @Builder.Default
    private List<TipReplyDTO> replies = new ArrayList<>();

    private boolean alreadyAddLike;     // 로그인 사용자가 이미 '좋아요' 눌렀는지
    private boolean alreadyAddDislike;  // 로그인 사용자가 이미 '싫어요' 눌렀는지

    // 로그인 사용자가 눌렀던 반응 상태 저장
// "like", "dislike", null
    private String userReaction;

    // 좋아요 카운트
    @Builder.Default
    private int likeCount = 0;      // primitive int 기본값 0 (좋아요 수)

    // 싫어요 카운트
    @Builder.Default
    private int dislikeCount = 0;   // primitive int 기본값 0 (싫어요 수)

    // 좋아요 DTO 생성
    public static TipBoardReactionDTO like(Long tipBoardId, String userId) {
        return TipBoardReactionDTO.builder()
                .tipBoardId(tipBoardId)
                .userId(userId)
                .reactionType(BoardReactionType.LIKE)
                .build();
    }

    // 싫어요 DTO 생성
    public static TipBoardReactionDTO dislike(Long tipBoardId, String userId) {
        return TipBoardReactionDTO.builder()
                .tipBoardId(tipBoardId)
                .userId(userId)
                .reactionType(BoardReactionType.DIS_LIKE)
                .build();
    }

    public TipBoardDTO toDTO(TipBoard entity, String memberId) {
        List<TipReplyDTO> replies = entity.getReplies() != null
                ? entity.getReplies().stream()
                .filter(r -> r != null && r.getReplyer() != null)
                .map(TipReplyDTO::fromEntity)
                .toList()
                : new ArrayList<>();

        boolean alreadyLike = entity.getReactions().stream()
                .anyMatch(r -> r.getUserId().equals(memberId) && r.getReaction() == BoardReactionType.LIKE);
        boolean alreadyDislike = entity.getReactions().stream()
                .anyMatch(r -> r.getUserId().equals(memberId) && r.getReaction() == BoardReactionType.DIS_LIKE);

        return TipBoardDTO.builder()
                .bno(entity.getBno())
                .title(entity.getTitle())
                .content(entity.getContent())
                .writer(entity.getWriter())      // 그냥 String
                .writerId(entity.getWriterId())  // Long 그대로
                .regDate(entity.getRegTime())
                .modDate(entity.getUpdateTime())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .dislikeCount(entity.getDislikeCount())
                .alreadyAddLike(alreadyLike)
                .alreadyAddDislike(alreadyDislike)
                .topHelpful(entity.getTopHelpful() != null && entity.getTopHelpful() == 1)
                .replies(replies)
                .build();
    }

    public static TipBoardDTO fromEntity(TipBoard tipBoard, String memberId) {
        // 댓글 리스트 변환
        List<TipReplyDTO> replyDTOs = tipBoard.getReplies() != null
                ? tipBoard.getReplies().stream().map(TipReplyDTO::fromEntity).toList()
                : new ArrayList<>();

        // 로그인 사용자가 좋아요/싫어요 눌렀는지 체크
        boolean alreadyLike = tipBoard.getReactions().stream()
                .anyMatch(r -> r.getUserId().equals(memberId) && r.getReaction() == BoardReactionType.LIKE);

        boolean alreadyDislike = tipBoard.getReactions().stream()
                .anyMatch(r -> r.getUserId().equals(memberId) && r.getReaction() == BoardReactionType.DIS_LIKE);

        // DTO 빌드
        return TipBoardDTO.builder()
                .bno(tipBoard.getBno())
                .title(tipBoard.getTitle())
                .content(tipBoard.getContent())
                .writer(tipBoard.getWriter())      // 그대로 String
                .writerId(tipBoard.getWriterId())  // 그대로 Long
                .regDate(tipBoard.getRegTime())
                .modDate(tipBoard.getUpdateTime())
                .viewCount(tipBoard.getViewCount())
                .likeCount(tipBoard.getLikeCount())
                .dislikeCount(tipBoard.getDislikeCount())
                .alreadyAddLike(alreadyLike)
                .alreadyAddDislike(alreadyDislike)
                .replies(replyDTOs)
                .build();
    }

}
