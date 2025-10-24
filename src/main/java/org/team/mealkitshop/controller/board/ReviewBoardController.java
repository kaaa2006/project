package org.team.mealkitshop.controller.board;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.ReviewBoardReply;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReviewBoardDTO;
import org.team.mealkitshop.dto.board.ReviewReplyDTO;
import org.team.mealkitshop.mapper.board.ReviewBoardMapper;
import org.team.mealkitshop.repository.board.ReviewBoardReplyRepository;
import org.team.mealkitshop.repository.board.ReviewBoardRepository;
import org.team.mealkitshop.service.board.ReviewBoardReactionService;
import org.team.mealkitshop.service.board.ReviewBoardService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/board/review")
@RequiredArgsConstructor
@Log4j2
public class ReviewBoardController {

    private final ReviewBoardService reviewBoardService;
    private final ReviewBoardMapper reviewBoardMapper;
    private final ReviewBoardRepository reviewBoardRepository;
    private final ReviewBoardReplyRepository reviewBoardReplyRepository;
    private final Rq rq; // 로그인 상태 및 회원 정보를 제공하는 컴포넌트
    private final ReviewBoardReactionService reviewBoardReactionService;

    // ==========================
    // 후기 게시글 리스트 조회 (모두 접근 가능)
    // ==========================
    @GetMapping("/list")
    public String list(PageRequestDTO pageRequestDTO, Model model) {
        try {
            Pageable pageable = pageRequestDTO.getPageable("regTime"); // bno 기준 정렬
            Page<ReviewBoard> result = reviewBoardRepository.findAll(pageable);

            List<ReviewBoardDTO> dtoList = result.getContent().stream()
                    .map(reviewBoardMapper::toDTO)
                    .toList();

            // ★ canViewSecret 세팅 (리스트에서 제목 마스킹/아이콘 용도)
            boolean isAdmin = rq.isLogined() && rq.isAdmin();
            String meEmail  = rq.isLogined() ? rq.getMember().getEmail() : null;
            dtoList.forEach(dto -> {
                boolean can = !dto.isSecretBoard()
                        || isAdmin
                        || (meEmail != null && meEmail.equalsIgnoreCase(dto.getWriterEmail()));
                dto.setCanViewSecret(can);
            });

            PageResponseDTO<ReviewBoardDTO> response =
                    PageResponseDTO.<ReviewBoardDTO>withAll()
                            .pageRequestDTO(pageRequestDTO)
                            .dtoList(dtoList)
                            .total((int) result.getTotalElements())
                            .build();

            model.addAttribute("response", response);
            model.addAttribute("isLoggedIn", rq.isLogined());
            model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());

            return "board/review/list";
        } catch (Exception e) {
            log.error("리뷰 리스트 조회 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "리뷰 목록을 불러오는 중 오류가 발생했습니다.");
            return "error/custom";
        }
    }

    // ==========================
    // 후기 게시글 상세 조회 (댓글 포함)
    // ==========================
    @GetMapping("/read")
    public String read(@RequestParam Long bno, Model model,
                       HttpServletRequest request, HttpServletResponse response) {

        try {
            ReviewBoard board = reviewBoardService.readBoardWithReplies(bno);
            if (board == null) return "redirect:/board/review/list";

            // 로그인 회원 정보
            Member loginMember = rq.isLogined() ? rq.getMember() : null;

            // DB 기준 비밀글 여부
            boolean isSecretBoard = board.isSecretBoard();

            // 로그인/관리자/작성자 체크
            boolean isAdmin = rq.isLogined() && rq.isAdmin();
            boolean isWriter = isWriter(board);

            // 기본 접근 권한
            boolean canViewSecret = !isSecretBoard || isAdmin || isWriter;

            // 비밀글 접근 권한 체크(비밀번호)
            if (isSecretBoard && !(isAdmin || isWriter)) {
                String pw = request.getParameter("password");
                if (pw != null && reviewBoardService.checkPassword(board, pw, loginMember)) {
                    canViewSecret = true;
                } else if (pw != null) {
                    model.addAttribute("passwordError", true);
                }
            }

            Long memberId = rq.isLogined() ? rq.getMemberMno() : null;

            // 조회수 증가
            if (memberId != null) {
                reviewBoardService.incrementViewCountForMember(board, memberId);
            } else {
                boolean alreadyViewed = false;
                if (request.getCookies() != null) {
                    for (var cookie : request.getCookies()) {
                        if (cookie.getName().equals("view_board_" + bno)) {
                            alreadyViewed = true;
                            break;
                        }
                    }
                }

                if (!alreadyViewed) {
                    board.setViewCount(board.getViewCount() + 1);
                    reviewBoardRepository.save(board);

                    var cookie = new Cookie("view_board_" + bno, "true");
                    cookie.setMaxAge(60 * 60 * 24);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }

            // DTO 변환
            ReviewBoardDTO reviewBoardDTO = reviewBoardMapper.toDTO(board);

            // ★ 상세 DTO에도 canViewSecret 반영 (뷰에서 DTO만으로 분기 가능)
            reviewBoardDTO.setCanViewSecret(canViewSecret);

            // 비밀글이면 title/content 마스킹
            if (!canViewSecret) {
                reviewBoardDTO.setTitle("비밀글입니다.");
                reviewBoardDTO.setContent(""); // 필요하면 내용 숨김
            }

            // 댓글 DTO 변환
            List<ReviewReplyDTO> replyDTOs = board.getReplies().stream()
                    .map(reply -> ReviewReplyDTO.builder()
                            .rno(reply.getRno())
                            .reviewBoardId(board.getBno())
                            .replyer(reply.getReplyer() != null ? reply.getReplyer().getMemberName() : "익명")
                            .replyText(reply.getReplyText() != null ? reply.getReplyText() : "")
                            .regDate(reply.getRegTime())
                            .secret(reply.isSecret())
                            // ⬇️ 관리자 뱃지 플래그
                            .admin(reply.getReplyer() != null && reply.getReplyer().getRole() == Role.ADMIN)
                            .build()
                    ).collect(Collectors.toList());

            reviewBoardDTO.setReplies(replyDTOs);

            // 최신 카운트 반영
            reviewBoardDTO.setHelpfulCount(reviewBoardReactionService.getHelpfulCount(bno));
            reviewBoardDTO.setNotHelpfulCount(reviewBoardReactionService.getNotHelpfulCount(bno));

            // 로그인한 사용자의 반응 상태
            if (rq.isLogined()) {
                String userReaction = reviewBoardReactionService.getUserReaction(
                        rq.getMemberId(),            // 로그인 사용자 ID
                        reviewBoardDTO.getBno()      // 게시글 ID
                ); // "helpful", "notHelpful", null
                reviewBoardDTO.setUserReaction(userReaction);
            }

            model.addAttribute("reviewBoardDTO", reviewBoardDTO);
            model.addAttribute("isLoggedIn", rq.isLogined());
            model.addAttribute("isWriter", isWriter);
            model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());

            // (기존 개별 플래그 유지 — 템플릿에서 편한 쪽 사용)
            model.addAttribute("canViewSecret", canViewSecret);
            model.addAttribute("secretBoard", isSecretBoard); // DB 기준

            return "board/review/read";

        } catch (Exception e) {
            log.error("게시글 조회 중 오류 발생 bno={} : {}", bno, e.getMessage(), e);
            model.addAttribute("errorMessage", "게시글 조회 중 오류가 발생했습니다.");
            return "error/custom";
        }
    }

    // ==========================
    // 게시글 등록 페이지 (로그인한 사용자만)
    // ==========================
    @GetMapping("/register")
    @PreAuthorize("hasRole('USER')")
    public String registerGET(Model model) {
        ReviewBoardDTO dto = new ReviewBoardDTO();
        dto.setWriter(rq.mustGetMember().getMemberName()); // 로그인 사용자 이름
        model.addAttribute("reviewBoardDTO", dto);
        return "board/review/register";
    }

    // ==========================
    // 게시글 등록 처리 (로그인한 사용자만)
    // ==========================
    @PostMapping("/register")
    @PreAuthorize("hasRole('USER')")
    public String registerPOST(@Valid ReviewBoardDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "board/review/register";

        Member loginMember = rq.mustGetMember();
        dto.setWriter(loginMember.getMemberName());

        Long bno = reviewBoardService.register(dto);
        return "redirect:/board/review/read?bno=" + bno;
    }

    // ==========================
    // 게시글 수정 (본인 글만)
    // ==========================
    @GetMapping("/modify")
    @PreAuthorize("hasRole('USER')")
    public String modifyGET(@RequestParam Long bno, Model model) {
        ReviewBoard board = reviewBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + bno));

        ReviewBoardDTO dto = reviewBoardMapper.toDTO(board);

        // 비밀글 체크 여부 추가
        dto.setSecretBoard(board.isSecretBoard());
        dto.setWasSecretBoard(board.isSecretBoard());
        model.addAttribute("reviewBoardDTO", dto);

        // 현재 로그인 사용자가 글 작성자인지 여부
        model.addAttribute("isWriter", isWriter(board));
        model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());

        return "board/review/modify";
    }

    @PostMapping("/modify")
    @PreAuthorize("hasRole('USER')")
    public String modifyPOST(@Valid ReviewBoardDTO dto,
                             @RequestParam(required=false) String currentPassword,
                             @RequestParam(required=false) String newPassword,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        // ⭐ 게시글을 먼저 가져와서 wasSecretBoard를 확정
        ReviewBoard board = reviewBoardRepository.findById(dto.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + dto.getBno()));
        dto.setWasSecretBoard(board.isSecretBoard());

        if (bindingResult.hasErrors()) {
            model.addAttribute("reviewBoardDTO", dto);
            model.addAttribute("isWriter", isWriter(board));
            model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());
            return "board/review/modify";
        }

        if (!isWriter(board)) return "redirect:/board/review/read?bno=" + dto.getBno();

        dto.setNewPassword(newPassword);

        if (dto.isSecretBoard()) {
            if (board.isSecretBoard()) {
                if (!reviewBoardService.checkPassword(board, currentPassword, rq.getMember())) {
                    bindingResult.rejectValue("currentPassword", "error.currentPassword", "비밀번호가 틀립니다.");
                    model.addAttribute("reviewBoardDTO", dto);
                    model.addAttribute("isWriter", true);
                    model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());
                    return "board/review/modify";
                }
                dto.setSecretPassword((newPassword != null && !newPassword.isBlank())
                        ? newPassword : board.getSecretPassword());
            } else {
                if (newPassword == null || newPassword.isBlank() || newPassword.length() < 4) {
                    bindingResult.rejectValue("newPassword", "error.newPassword", "비밀글 설정 시 비밀번호는 최소 4자 이상이어야 합니다.");
                    model.addAttribute("reviewBoardDTO", dto);
                    model.addAttribute("isWriter", true);
                    model.addAttribute("isAdmin", rq.isLogined() && rq.isAdmin());
                    return "board/review/modify";
                }
                dto.setSecretPassword(newPassword);
            }
        } else {
            dto.setSecretPassword(null);
        }

        reviewBoardService.modify(dto);
        redirectAttributes.addFlashAttribute("msg", "리뷰가 수정되었습니다.");
        return "redirect:/board/review/read?bno=" + dto.getBno();
    }


    // ==========================
    // 게시글 삭제 (본인 글만)
    // ==========================
    @PostMapping("/remove")
    @PreAuthorize("hasRole('USER')")
    public String remove(@RequestParam Long bno, RedirectAttributes redirectAttributes) {
        try {
            ReviewBoard board = reviewBoardRepository.findById(bno)
                    .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + bno));

            if (!isWriter(board)) {
                redirectAttributes.addFlashAttribute("msg", "본인 글만 삭제할 수 있습니다.");
                return "redirect:/board/review/read?bno=" + bno;
            }

            reviewBoardService.remove(bno, rq.mustGetMember());
            redirectAttributes.addFlashAttribute("msg", "리뷰가 삭제되었습니다.");
            return "redirect:/board/review/list";

        } catch (IllegalStateException e) { // 댓글 존재
            redirectAttributes.addFlashAttribute("msg", e.getMessage());
            return "redirect:/board/review/read?bno=" + bno; // 이전 페이지로
        }
    }

    // ==========================
    // 댓글 등록/삭제/수정 (관리자만)
    // ==========================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reply/register")
    @ResponseBody
    public ReviewReplyDTO registerReply(@RequestParam Long bno,
                                        @RequestParam String replyText) {

        Member loginMember = rq.mustGetMember();
        ReviewBoard board = reviewBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + bno));

        ReviewBoardReply reply = ReviewBoardReply.builder()
                .reviewBoard(board)
                .replyer(loginMember)
                .replyText(replyText)
                .secret(false)
                .build();

        ReviewBoardReply saved = reviewBoardReplyRepository.save(reply);

        return ReviewReplyDTO.builder()
                .rno(saved.getRno())
                .reviewBoardId(bno)
                .replyer(loginMember.getMemberName())
                .replyText(saved.getReplyText())
                .regDate(saved.getRegTime())
                .secret(saved.isSecret())
                .admin(loginMember.getRole() == Role.ADMIN)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reply/remove")
    @ResponseBody
    public String removeReply(@RequestParam Long rno) {
        ReviewBoardReply reply = reviewBoardReplyRepository.findById(rno)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음 rno=" + rno));

        reviewBoardReplyRepository.delete(reply);
        return "댓글 삭제 완료";
    }

    // ==========================
    // Helper: 현재 로그인한 회원이 글 작성자인지 확인
    // ==========================
    private boolean isWriter(ReviewBoard board) {
        if (!rq.isLogined()) return false;
        Member me = rq.getMember();
        if (board.getWriterMember() != null) {
            return board.getWriterMember().getMno().equals(me.getMno());
        }
        // 보조: 문자열 비교(레거시/이행기 대비)
        return me.getMemberName() != null && me.getMemberName().equals(board.getWriter());
    }

    @GetMapping("/list-json")
    @ResponseBody
    @PermitAll
    public ResponseEntity<List<Map<String, Object>>> listJson() {
        List<Map<String, Object>> dtoList = reviewBoardRepository
                .findTop50ByOrderByRegTimeDesc()
                .stream()
                .map(board -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bno", board.getBno());
                    boolean can = !board.isSecretBoard() || rq.isLoginedOrAdminOrWriter(board); // ★ JSON에도 동일 로직
                    if (board.isSecretBoard() && !can) {
                        map.put("title", "비밀글입니다.");
                    } else {
                        map.put("title", board.getTitle());
                    }
                    map.put("writer", board.getWriter());
                    map.put("writerEmail", board.getWriterMember() != null ? board.getWriterMember().getEmail() : "");
                    map.put("viewCount", board.getViewCount());
                    map.put("helpfulCount", board.getHelpfulCount());
                    map.put("notHelpfulCount", board.getNotHelpfulCount());
                    map.put("regDate", board.getRegTime());
                    map.put("modDate", board.getUpdateTime());
                    map.put("secretBoard", board.isSecretBoard());
                    map.put("canViewSecret", can); // ★ 선택: 클라이언트가 활용 가능
                    return map;
                })
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/remove-json")
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String,Object>> removeJson(@RequestParam Long bno) {
        Map<String,Object> res = new HashMap<>();
        try {
            ReviewBoard board = reviewBoardRepository.findById(bno)
                    .orElseThrow(() -> new IllegalArgumentException("게시글 없음 bno=" + bno));

            if (!isWriter(board)) {
                res.put("success", false);
                res.put("message", "본인 글만 삭제할 수 있습니다.");
                return ResponseEntity.ok(res);
            }

            reviewBoardService.remove(bno, rq.mustGetMember());
            res.put("success", true);
            res.put("message", "리뷰가 삭제되었습니다.");
            return ResponseEntity.ok(res);

        } catch (IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "리뷰 삭제 실패");
            return ResponseEntity.ok(res);
        }
    }

    @GetMapping("/check-password")
    @ResponseBody
    public Map<String, Object> checkPassword(@RequestParam Long bno,
                                             @RequestParam String password) {

        ReviewBoard reviewBoard = reviewBoardService.readBoardWithReplies(bno);
        Map<String, Object> result = new HashMap<>();

        Member loginMember = rq.isLogined() ? rq.getMember() : null;
        boolean isValid = reviewBoardService.checkPassword(reviewBoard, password, loginMember);

        result.put("success", isValid);
        return result;
    }
}
