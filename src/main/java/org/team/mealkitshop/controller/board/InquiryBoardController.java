package org.team.mealkitshop.controller.board;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.dto.board.AdminInquiryDTO;
import org.team.mealkitshop.dto.board.UserInquiryDTO;
import org.team.mealkitshop.service.board.AdminInquiryService;
import org.team.mealkitshop.service.board.UserInquiryService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inquiry")
@RequiredArgsConstructor
public class InquiryBoardController {
    // 1:1 문의 Controller
    // 사용자(User)용 API
    // 관리자(Admin)용 API

    private final UserInquiryService userInquiryService;
    private final AdminInquiryService adminInquiryService;
    private final Rq rq;

    // ==========================
    // 사용자(User)용 API
    // ==========================

    /** 로그인한 사용자의 내 문의 리스트 조회 */
    @GetMapping("/my")
    @ResponseBody
    public ResponseEntity<List<UserInquiryDTO>> getMyInquiries() {
        Long userId = rq.mustGetMember().getMno(); // 로그인 유저 ID
        List<UserInquiryDTO> inquiries = userInquiryService.getMyInquiries(userId);
        return ResponseEntity.ok(inquiries);
    }

    /** 1:1 문의 작성 */
    @PutMapping("/my/{inquiryId}")
    @ResponseBody
    public ResponseEntity<String> editInquiry(
            @PathVariable Long inquiryId,
            @RequestBody Map<String, String> payload) {

        Long userId = rq.mustGetMember().getMno();
        String title = payload.get("title");     // ✅ title 가져오기
        String content = payload.get("content"); // ✅ content 가져오기

        try {
            userInquiryService.editInquiry(inquiryId, userId, title, content); // ✅ title도 같이 전달
            return ResponseEntity.ok("수정되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("수정에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/my")
    @ResponseBody
    public ResponseEntity<Long> addInquiry(@RequestBody Map<String, String> payload) {
        Long userId = rq.mustGetMember().getMno();
        String title = payload.get("title");
        String content = payload.get("content");
        Long inquiryId = userInquiryService.addInquiry(userId, title, content);
        return ResponseEntity.ok(inquiryId);
    }

    /** 1:1 문의 삭제 - 답변이 달린 문의는 삭제 불가 */
    @DeleteMapping("/my/{inquiryId}")
    @ResponseBody
    public ResponseEntity<String> deleteInquiry(@PathVariable Long inquiryId) {
        Long userId = rq.mustGetMember().getMno();
        userInquiryService.deleteInquiry(inquiryId, userId);
        return ResponseEntity.ok("문의가 삭제되었습니다");
    }

    /** 내 문의 리스트 페이지 반환 (Thymeleaf) */
    @GetMapping("/my/list")
    public String myInquiryList(Model model) {
        Long userId = rq.mustGetMember().getMno();
        model.addAttribute("inquiries", userInquiryService.getMyInquiries(userId));
        return "board/inquiry/my/list"; // ← 여기를 수정
    }

    /** 내 문의 상세 페이지 조회 */
    @GetMapping("/my/{inquiryId}")
    public String myInquiryRead(@PathVariable Long inquiryId, Model model) {
        Long userId = rq.mustGetMember().getMno();
        UserInquiryDTO dto = userInquiryService.getInquiryDetail(inquiryId, userId);

        if(dto == null){
            return "redirect:/inquiry/my/list"; // 존재하지 않으면 리스트로
        }

        model.addAttribute("dto", dto); // ✅ dto를 모델에 담아야 함
        return "board/inquiry/my/read"; // read.html 경로
    }

    @GetMapping("/my/{inquiryId}/json")
    @ResponseBody
    public ResponseEntity<UserInquiryDTO> getInquiryDetailJson(@PathVariable Long inquiryId) {
        Long userId = rq.mustGetMember().getMno();
        UserInquiryDTO dto = userInquiryService.getInquiryDetail(inquiryId, userId);
        return ResponseEntity.ok(dto);
    }

    // ==========================
    // 관리자(Admin)용 API
    // ==========================

    /** 모든 1:1 문의 조회 (관리자) */
    @GetMapping("/admin")
    public ResponseEntity<List<AdminInquiryDTO>> getAllInquiries() {
        if (!rq.isAdmin()) {
            return ResponseEntity.status(403).build(); // 권한 체크
        }
        List<AdminInquiryDTO> inquiries = adminInquiryService.getAllInquiries();
        return ResponseEntity.ok(inquiries);
    }

    /** 관리자용 문의 상세 페이지 */
    @GetMapping("/admin/{inquiryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminInquiryRead(@PathVariable Long inquiryId, Model model) {
        AdminInquiryDTO dto = adminInquiryService.getInquiryDetail(inquiryId);
        model.addAttribute("dto", dto);
        return "board/inquiry/admin/read"; // read.html
    }

    /** 문의 답변 작성 또는 수정 (관리자) */
    @PostMapping("/admin/answer")
    public ResponseEntity<String> addOrUpdateAnswer(
            @RequestParam Long inquiryId,
            @RequestParam String content) {

        if (!rq.isAdmin()) {
            return ResponseEntity.status(403).build(); // 권한 체크
        }

        Long adminId = rq.mustGetMember().getMno(); // 로그인 관리자 ID
        adminInquiryService.addOrUpdateAnswer(inquiryId, adminId, content);
        return ResponseEntity.ok("답변이 등록/수정되었습니다");
    }

    /** 관리자용 답변 삭제 */
    @DeleteMapping("/admin/answer/{inquiryId}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAnswer(@PathVariable Long inquiryId) {

        try {
            adminInquiryService.deleteAnswer(inquiryId); // 실제 삭제 로직 호출
            return ResponseEntity.ok("답변이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("삭제에 실패했습니다: " + e.getMessage());
        }
    }

    // 관리자용 1:1 문의 페이지 (GET 뷰)
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminInquiryPage(Model model) {

        // 모든 문의 가져오기
        List<AdminInquiryDTO> inquiries = adminInquiryService.getAllInquiries();

        model.addAttribute("inquiries", inquiries);

        return "board/inquiry/admin/list"; // list.html로 변경
    }

    /** 관리자용 문의 상세 JSON 반환 (AJAX용) */
    @GetMapping("/admin/{inquiryId}/json")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public AdminInquiryDTO adminInquiryDetailJson(@PathVariable Long inquiryId) {
        return adminInquiryService.getInquiryDetail(inquiryId);
    }

}
