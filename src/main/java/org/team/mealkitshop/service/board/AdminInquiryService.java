package org.team.mealkitshop.service.board;

import org.team.mealkitshop.dto.board.AdminInquiryDTO;

import java.util.List;

public interface AdminInquiryService {
    // 관리자용 1:1 문의 서비스 인터페이스

    // 모든 문의 조회
    List<AdminInquiryDTO> getAllInquiries();

    // 답변 작성/수정
    void addOrUpdateAnswer(Long inquiryId, Long adminId, String content);

    // 답변 삭제
    void deleteAnswer(Long inquiryId);

    AdminInquiryDTO getInquiryDetail(Long inquiryId);
}
