package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.team.mealkitshop.common.AnswerStatus;
import org.team.mealkitshop.domain.board.InquiryAnswer;
import org.team.mealkitshop.domain.board.InquiryBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.AdminInquiryDTO;
import org.team.mealkitshop.repository.board.InquiryBoardRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminInquiryServiceImpl implements AdminInquiryService {
    // 관리자용 1:1 문의 서비스 구현체
    // 모든 문의 조회
    // 답변 작성/수정

    private final InquiryBoardRepository inquiryBoardRepository;
    private final MemberRepository memberRepository;

    // 모든 1:1 문의 조회
     // 관리자는 모든 사용자의 문의를 조회 가능
     // 답변이 있으면 답변 내용 포함
     // 상태(PENDING / ANSWERED) 포함
    @Override
    public List<AdminInquiryDTO> getAllInquiries() {
        List<InquiryBoard> inquiries = inquiryBoardRepository.findAllWithWriterAndAnswer();

        // InquiryBoard → AdminInquiryDTO 변환 (fromEntity 사용, status 한글)
        return inquiries.stream()
                .map(AdminInquiryDTO::fromEntity)
                .toList();
    }

    // 문의 답변 작성 또는 수정
    // 답변이 없으면 새로 생성
    // 이미 답변이 있으면 내용 수정
    // 상태를 ANSWERED로 변경
    // @param inquiryId 답변할 문의 ID
    // @param adminId 답변 작성 관리자 ID
    // @param content 답변 내용
    @Override
    public void addOrUpdateAnswer(Long inquiryId, Long adminId, String content) {
        // 1️⃣ 문의 조회
        InquiryBoard board = inquiryBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다"));

        // 관리자(Member) 조회
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 회원이 없습니다"));

        // 2️⃣ 답변이 없는 경우 새로 생성
        if (board.getAnswer() == null) {
            InquiryAnswer answer = InquiryAnswer.builder()
                    .inquiryBoard(board)                           // 어떤 문의에 대한 답변인지 설정
                    .admin(admin)  // Member 엔티티 연결 - 답변 작성 관리자 연결
                    .content(content)                              // 답변 내용
                    .build();

            // 문의에 답변 설정
            board.setAnswer(answer);
        } else {
            // 3️⃣ 이미 답변이 있는 경우 내용 수정
            board.getAnswer().setContent(content);
        }

        // 4️⃣ 답변 상태 변경 (PENDING → ANSWERED)
        board.setStatus(AnswerStatus.ANSWERED);

        // 5️⃣ 변경사항 저장
        inquiryBoardRepository.save(board);
    }

    @Override
    public void deleteAnswer(Long inquiryId) {
        InquiryBoard board = inquiryBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다"));

        if (board.getAnswer() != null) {
            board.setAnswer(null);
            board.setStatus(AnswerStatus.PENDING); // 답변 삭제 후 상태 초기화
            inquiryBoardRepository.save(board); // <- 여기서 실제 DB에 반영
        }
    }

    @Override
    public AdminInquiryDTO getInquiryDetail(Long inquiryId) {
        InquiryBoard inquiryBoard = inquiryBoardRepository.findByIdWithWriterAndAnswer(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의글을 찾을 수 없습니다: " + inquiryId));

        return AdminInquiryDTO.fromEntity(inquiryBoard);
    }

}