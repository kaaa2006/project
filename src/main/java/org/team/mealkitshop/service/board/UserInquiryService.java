package org.team.mealkitshop.service.board;

import org.team.mealkitshop.dto.board.UserInquiryDTO;

import java.util.List;

// 사용자용 1:1 문의 서비스 인터페이스
public interface UserInquiryService {

    // 로그인한 사용자의 문의 조회
    List<UserInquiryDTO> getMyInquiries(Long userId);

    // 1:1 문의 작성
    Long addInquiry(Long userId, String title, String content);

    // 1:1 문의 수정
    void editInquiry(Long inquiryId, Long userId, String title, String content);

    // 1:1 문의 삭제 - 답변 없는 문의만 삭제 가능
    void deleteInquiry(Long inquiryId, Long userId);

    // 1:1 문의 상세 조회
    UserInquiryDTO getInquiryDetail(Long inquiryId, Long userId);

}
