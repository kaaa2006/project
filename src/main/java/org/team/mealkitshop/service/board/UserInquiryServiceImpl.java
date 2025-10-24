package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.team.mealkitshop.common.AnswerStatus;
import org.team.mealkitshop.domain.board.InquiryBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.UserInquiryDTO;
import org.team.mealkitshop.repository.board.InquiryBoardRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInquiryServiceImpl implements UserInquiryService {
    // 사용자용 1:1 문의 서비스 구현체
    // 로그인한 사용자가 자신의 문의만 조회 가능
    // 문의 작성, 삭제 가능
    // 삭제 시 답변이 달린 문의는 삭제 제한

    private final InquiryBoardRepository inquiryBoardRepository;

    /**
     * 로그인한 사용자의 1:1 문의 조회
     * - 답변이 있으면 답변 내용 포함
     * - 상태(PENDING / ANSWERED) 포함
     *
     * @param userId 로그인한 사용자 ID
     * @return 사용자 문의 리스트
     */
    @Override
    public List<UserInquiryDTO> getMyInquiries(Long userId) {
        // 1️⃣ DB에서 로그인한 사용자의 문의 조회
        List<InquiryBoard> inquiries = inquiryBoardRepository.findByWriterMnoWithAnswer(userId);

        // 2️⃣ InquiryBoard → UserInquiryDTO 변환
        return inquiries.stream()
                .map(board -> UserInquiryDTO.builder()
                        .id(board.getId())                                       // 문의 ID
                        .title(board.getTitle())                                  // 문의 제목
                        .content(board.getContent())                              // 문의 내용
                        .answerContent(board.getAnswer() != null ? board.getAnswer().getContent() : null) // 답변 내용
                        .status(getStatusText(board.getStatus()))
                        .userName(board.getWriter() != null ? board.getWriter().getMemberName() : "알 수 없음")// 답변 상태
                        .regDate(board.getRegTime())      // ✅ 등록일 매핑
                        .modDate(board.getUpdateTime())   // ✅ 수정일 매핑
                        .answerDate(board.getAnswer() != null ? board.getAnswer().getRegTime() : null)
                        .build()
                ).collect(Collectors.<UserInquiryDTO>toList());
    }

    /**
     * 1:1 문의 작성
     *
     * @param userId 로그인한 사용자 ID
     * @param title 문의 제목
     * @param content 문의 내용
     * @return 생성된 문의 ID
     */
    @Override
    public Long addInquiry(Long userId, String title, String content) {
        // 1️⃣ InquiryBoard 엔티티 생성
        InquiryBoard board = InquiryBoard.builder()
                .writer(Member.builder().mno(userId).build())
                .title(title)
                .content(content)
                .status(AnswerStatus.PENDING)
                .regTime(LocalDateTime.now())      // 작성일 추가
                .updateTime(LocalDateTime.now())   // 수정일 추가
                .build();

        // 2️⃣ DB 저장
        inquiryBoardRepository.save(board);

        // 3️⃣ 생성된 문의 ID 반환
        return board.getId();
    }

    /**
     * 1:1 문의 수정
     */
    @Override
    public void editInquiry(Long inquiryId, Long userId, String title, String content) {
        // 1️⃣ 해당 문의 조회 (본인 확인)
        InquiryBoard board = inquiryBoardRepository.findByIdAndWriterMnoWithAnswer(inquiryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다"));

        // 2️⃣ 답변이 달린 문의는 수정 불가
        if (board.getAnswer() != null) {
            throw new IllegalStateException("답변이 달린 문의는 수정할 수 없습니다");
        }

        // 3️⃣ 제목 수정, 내용 수정 및 수정일 업데이트
        board.setTitle(title);
        board.setContent(content);
        board.setUpdateTime(LocalDateTime.now());

        // 4️⃣ DB 저장
        inquiryBoardRepository.save(board);
    }

    /**
     * 1:1 문의 삭제
     * - 답변이 달린 문의는 삭제 불가
     *
     * @param inquiryId 삭제할 문의 ID
     * @param userId 로그인한 사용자 ID
     */
    @Override
    public void deleteInquiry(Long inquiryId, Long userId) {
        // 1️⃣ 해당 문의 조회 (작성자 본인 확인)
        InquiryBoard board = inquiryBoardRepository.findByIdAndWriterMnoWithAnswer(inquiryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다"));

        // 2️⃣ 답변이 있으면 삭제 제한
        if (board.getAnswer() != null) {
            throw new IllegalStateException("답변이 달린 문의는 삭제할 수 없습니다");
        }

        // 3️⃣ 문의 삭제
        inquiryBoardRepository.delete(board);
    }

    /**
     * 1:1 문의 상세 조회
     * - 로그인한 사용자 본인의 문의만 조회 가능
     *
     * @param inquiryId 조회할 문의 ID
     * @param userId 로그인한 사용자 ID
     * @return UserInquiryDTO
     */
    @Override
    public UserInquiryDTO getInquiryDetail(Long inquiryId, Long userId) {
        InquiryBoard board = inquiryBoardRepository.findByIdAndWriterMnoWithAnswer(inquiryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다"));

        return UserInquiryDTO.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .regDate(board.getRegTime()) // 문의 등록일
                .modDate(board.getUpdateTime()) // 수정일
                .answerContent(board.getAnswer() != null ? board.getAnswer().getContent() : null)
                .answerDate(board.getAnswer() != null ? board.getAnswer().getRegTime() : null) // 답변일
                .status(getStatusText(board.getStatus()))
                .userName(board.getWriter() != null ? board.getWriter().getMemberName() : "알 수 없음")
                .build();
    }

    // ==========================
// 상태 한글 변환 메서드
// ==========================
    private String getStatusText(AnswerStatus status) {
        if (status == null) return "-";
        switch (status) {
            case PENDING: return "답변 대기";
            case ANSWERED: return "답변 완료";
            default: return status.name(); // 혹시 모르는 상태
        }
    }
}