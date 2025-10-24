package org.team.mealkitshop.controller.board;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.TipBoardDTO;
import org.team.mealkitshop.dto.board.TipBoardReactionDTO;
import org.team.mealkitshop.repository.board.TipBoardRepository;
import org.team.mealkitshop.service.board.TipBoardReactionService;
import org.team.mealkitshop.service.board.TipBoardService;

import java.util.ArrayList;
import java.util.List;

/**
 * TIP 게시판 컨트롤러
 *
 * 권한 정책:
 * - 조회/리스트 : 모든 사용자 접근 가능
 * - 글쓰기/수정/삭제 : USER 권한만 가능
 * - 본인 글만 수정/삭제 가능
 * - 좋아요/싫어요 : USER 권한만 가능
 */
@Controller
@RequestMapping("/board/tip")
@RequiredArgsConstructor
@Log4j2
public class TipBoardController {

    private final TipBoardService tipBoardService;
    private final TipBoardReactionService tipBoardReactionService;
    private final Rq rq; // 현재 로그인된 회원 정보를 제공
    private final TipBoardRepository tipBoardRepository;

    /**
     * TIP 게시판 리스트 조회
     * - 모든 사용자가 접근 가능
     * - 로그인 여부(isLoggedIn) 모델에 추가 (글쓰기 버튼 UI 제어용)
     * - 최근 TIP 게시글 목록 모델에 추가
     */
    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("isLoggedIn", rq.isLogined());
        model.addAttribute("tipList", tipBoardService.getRecentTips());
        return "board/tip/list"; // 리스트 뷰 반환
    }

    /**
     * TIP 게시판 리스트 JSON 반환 (REST API용)
     * - 모든 사용자 접근 가능
     * - Ajax 등에서 사용
     */
    @GetMapping("/list-json")
    @ResponseBody
    public ResponseEntity<List<TipBoardDTO>> listJson() {

        // 로그인 회원 확인
        Member member = rq.isLogined() ? rq.getMember() : null;
        String memberId = member != null ? member.getMno().toString() : null;

        // DTO 변환
        List<TipBoardDTO> tipDTOs = tipBoardService.getRecentTips()
                .stream()
                .map(board -> TipBoardDTO.fromEntity(board, memberId))
                .toList();

        return ResponseEntity.ok(tipDTOs);
    }

    /**
     * TIP 게시글 상세 조회
     * - 모든 사용자가 접근 가능
     * - 로그인 여부 및 로그인한 회원 ID 전달
     * - 댓글 목록은 Ajax로 가져오기 때문에 여기서는 bno와 memberId만 전달
     */
    @GetMapping("/read")
    public String read(@RequestParam Long bno, Model model) {

        // 로그인 회원 확인
        Member member = rq.isLogined() ? rq.getMember() : null;
        String memberId = member != null ? member.getMno().toString() : null;

        // 게시글 DTO 가져오기 (memberId 포함, 좋아요/싫어요 상태 체크)
        TipBoardDTO tipBoardDTO = tipBoardService.readOne(bno, memberId);

        log.info("tipBoardDTO={}", tipBoardDTO);

        // null 체크
        if (tipBoardDTO == null) {
            log.warn("bno={}에 해당하는 TipBoard가 존재하지 않습니다.", bno);
            return "redirect:/board/tip/list";
        }

// 좋아요/싫어요 체크
        boolean alreadyLike = false;
        boolean alreadyDislike = false;
        if (member != null) {
            alreadyLike = tipBoardReactionService.isAlreadyAddGoodRp(bno, member.getMno().toString());
            alreadyDislike = tipBoardReactionService.isAlreadyAddBadRp(bno, member.getMno().toString());
        }
        tipBoardDTO.setAlreadyAddLike(alreadyLike);
        tipBoardDTO.setAlreadyAddDislike(alreadyDislike);

        // 로그인 사용자가 눌렀던 반응 상태 세팅
        if (alreadyLike) tipBoardDTO.setUserReaction("like");
        else if (alreadyDislike) tipBoardDTO.setUserReaction("dislike");
        else tipBoardDTO.setUserReaction(null);

// null-safe 필드 초기화
        if (tipBoardDTO.getReplies() == null) tipBoardDTO.setReplies(new ArrayList<>());

        model.addAttribute("tipBoardDTO", tipBoardDTO);
        model.addAttribute("isLoggedIn", rq.isLogined());
        model.addAttribute("memberIdLong", member != null ? member.getMno() : null);
        model.addAttribute("memberId", memberId);

        // 댓글 목록은 Ajax로 TipReplyController에서 가져오기 때문에 여기서는 추가하지 않음
        return "board/tip/read";
    }

    /**
     * TIP 게시글 글쓰기 페이지 (GET)
     * - USER 권한만 접근 가능
     * - TipBoardDTO 모델에 추가 (Thymeleaf form 바인딩용)
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/register")
    public void registerGET(Model model) {
        TipBoardDTO tipBoardDTO = TipBoardDTO.builder()
                .writer(rq.getMember().getMemberName()) // 작성자 이름 강제 세팅
                .build();
        model.addAttribute("tipBoardDTO", tipBoardDTO);
    }

    /**
     * TIP 게시글 등록 처리 (POST)
     * - USER 권한만 접근 가능
     * - 작성자(Member)를 강제로 DTO에 반영
     * - 등록 후 리스트 페이지로 리다이렉트
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/register")
    public String registerPost(@ModelAttribute TipBoardDTO tipBoardDTO,
                               RedirectAttributes redirectAttributes) {
        Member member = rq.getMember();

        // ✅ 등록 DTO에 작성자 ID/이름 세팅
        tipBoardDTO.setWriterId(member.getMno());
        tipBoardDTO.setWriter(member.getMemberName());

        Long bno = tipBoardService.register(tipBoardDTO, member); // 등록
        redirectAttributes.addFlashAttribute("result", bno); // 결과 메시지
        return "redirect:/board/tip/list";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/modify")
    public String modifyGET(@RequestParam Long bno, Model model) {
        Member member = rq.getMember();
        TipBoardDTO tipBoardDTO = tipBoardService.readOne(bno, member.getMno().toString());

        // 본인 글인지 체크
        if (!tipBoardDTO.getWriter().equals(member.getMemberName())) {
            throw new IllegalArgumentException("본인 글만 수정 가능");
        }

        model.addAttribute("dto", tipBoardDTO); // 수정 폼에서 dto 사용
        return "board/tip/modify"; // 수정 페이지 뷰
    }

    /**
     * TIP 게시글 수정 처리 (POST)
     * - USER 권한만 접근 가능
     * - 본인 글인지 검증 후 수정
     * - 수정 후 상세 페이지로 리다이렉트
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/modify")
    public String modify(@ModelAttribute TipBoardDTO tipBoardDTO,
                         RedirectAttributes redirectAttributes) {
        Member member = rq.getMember();
        tipBoardService.modify(tipBoardDTO, member); // 본인 글만 수정
        redirectAttributes.addFlashAttribute("result", "modified");
        return "redirect:/board/tip/read?bno=" + tipBoardDTO.getBno();
    }

    /**
     * TIP 게시글 삭제 처리 (POST)
     * - USER 권한만 접근 가능
     * - 본인 글인지 검증 후 삭제
     * - 관련 댓글/반응도 함께 삭제
     * - 삭제 후 리스트 페이지로 리다이렉트
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/remove")
    public String remove(@RequestParam Long bno, RedirectAttributes redirectAttributes) {
        Member member = rq.getMember();
        tipBoardService.remove(bno, member);
        redirectAttributes.addFlashAttribute("result", "removed");
        return "redirect:/board/tip/list";
    }

    /**
     * TIP 게시글 좋아요/싫어요 토글 처리 (REST API)
     * - USER 권한만 접근 가능
     * - TipBoardReactionDTO를 RequestBody로 전달받아 서비스에서 처리
     * - Ajax 호출용
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/react")
    @ResponseBody
    public ResponseEntity<String> reactTipBoard(@RequestBody TipBoardReactionDTO tipBoardReactionDTO) {
        tipBoardReactionService.toggleReaction(tipBoardReactionDTO);
        return ResponseEntity.ok("TipBoard 반응 처리 완료");
    }

    @PostMapping("/increment-view/{bno}")
    @ResponseBody
    @Transactional
    public void incrementView(@PathVariable Long bno, HttpServletRequest request, HttpServletResponse response) {
        TipBoard board = tipBoardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        if (rq.isLogined()) {
            Long memberId = rq.getMember().getMno();
            tipBoardService.incrementViewCountForMember(board, memberId);
        } else {
            // 비로그인 쿠키 체크
            boolean alreadyViewed = false;
            if (request.getCookies() != null) {
                for (var cookie : request.getCookies()) {
                    if (cookie.getName().equals("view_tip_" + bno)) {
                        alreadyViewed = true;
                        break;
                    }
                }
            }

            if (!alreadyViewed) {
                board.setViewCount(board.getViewCount() + 1);
                tipBoardRepository.save(board);

                var cookie = new Cookie("view_tip_" + bno, "true");
                cookie.setMaxAge(60 * 60 * 24); // 하루
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
    }

}