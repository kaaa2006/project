package org.team.mealkitshop.controller.board;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.component.Rq;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.*;
import org.team.mealkitshop.service.board.BoardImageService;
import org.team.mealkitshop.service.board.BoardService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/board")
@Log4j2
@RequiredArgsConstructor
public class BoardController {

    private final BoardImageService boardImageService;
    @Value("${uploadPath}")
    private String uploadPath;

    private final BoardService boardService;
    private final Rq rq;

    /** 게시판 리스트 조회 */
    @GetMapping("/list")
    public String list(@RequestParam(required = false, defaultValue = "EVENT") BoardType boardType,
                       @RequestParam(required = false, defaultValue = "ongoing") String status,
                       PageRequestDTO pageRequestDTO,
                       Model model) {

        // Service에서 status에 따라 조회
        PageResponseDTO<BoardDTO> responseDTO = boardService.listByType(boardType, pageRequestDTO, status);

        model.addAttribute("responseDTO", responseDTO);

        // 이벤트 게시판이면 events 이름으로 model에 전달
        if(boardType == BoardType.EVENT){
            model.addAttribute("events", responseDTO.getDtoList());
        }

        model.addAttribute("boardType", boardType);
        model.addAttribute("status", status);
        model.addAttribute("pageRequestDTO", pageRequestDTO);

        return switch(boardType) {
            case FAQ -> "board/faq/list";
            case EVENT -> "board/event/list";
            default -> "board/board/list";
        };
    }

    /** 관리자 게시글 작성 페이지 이동 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/register")
    public String registerGET(@RequestParam(required = false, defaultValue = "NOTICE") BoardType boardType, Model model){

        BoardDTO boardDTO = BoardDTO.builder()
                .boardType(boardType)
                .build();
        model.addAttribute("boardDTO", boardDTO);
        model.addAttribute("boardTypes", BoardType.values());

        return "board/board/register";
    }

    /** 관리자 게시글 등록 처리 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public String registerPost(@Valid BoardDTO boardDTO,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        System.out.println("===== System.out.println 실행 =====");
        log.info("===== registerPost 실행됨 =====");

        // validation 오류 체크
        if(bindingResult.hasErrors()) {
            log.error("검증 실패: {}", bindingResult.getAllErrors());
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("boardDTO", boardDTO);
            model.addAttribute("boardTypes", BoardType.values());
            return "board/board/register";   // 에러 있으면 등록 페이지로 다시
        }

        if(boardDTO.getBoardType() == null) boardDTO.setBoardType(BoardType.NOTICE);

        // ---------- EVENT 날짜 처리 ----------
        if(boardDTO.getBoardType() == BoardType.EVENT) {
            boardDTO.setStartDate(LocalDate.now()); // 시작일 무조건 오늘

            // 종료일을 입력하지 않았거나 과거면 오류
            if(boardDTO.getEndDate() == null) {
                bindingResult.rejectValue("endDate", "required", "종료일을 입력해야 합니다.");
                model.addAttribute("boardDTO", boardDTO);
                model.addAttribute("boardTypes", BoardType.values());
                return "board/board/register";
            }

            if(boardDTO.getEndDate().isBefore(LocalDate.now())) {
                bindingResult.rejectValue("endDate", "invalid", "종료일은 오늘 이후 날짜여야 합니다.");
                model.addAttribute("boardDTO", boardDTO);
                model.addAttribute("boardTypes", BoardType.values());
                return "board/board/register";
            }

            boardDTO.setExpired(boardDTO.getEndDate().isBefore(LocalDate.now()));
        }
        // -----------------------------------



        Member member = rq.mustGetMember();

        List<MultipartFile> files = (boardDTO.getBoardType() == BoardType.EVENT || boardDTO.getBoardType() == BoardType.NOTICE)
                ? boardDTO.getFiles()
                : null;

        // 1. 게시글 먼저 저장 (이미지 제외)
        Long bno = boardService.register(boardDTO, member, null);

// 2. 이미지가 있으면 저장
        if(files != null && !files.isEmpty()) {
            boardImageService.saveImages(bno, files);
        }

        redirectAttributes.addAttribute("boardType", boardDTO.getBoardType());
        redirectAttributes.addFlashAttribute("result", "registered");
        return "redirect:/board/read?bno=" + bno + "&boardType=" + boardDTO.getBoardType();
    }

    /** 게시글 읽기 */
    @GetMapping("/read")
    public String read(@RequestParam Long bno,
                       @RequestParam(required = false) BoardType boardType,
                       PageRequestDTO pageRequestDTO,
                       Model model,
                       HttpServletRequest request,
                       HttpServletResponse response){

        Optional<BoardDTO> boardOpt = boardService.readOptional(bno);

        if (boardOpt.isEmpty() || boardOpt.get().isDeleted()) {
            model.addAttribute("message", "삭제된 게시글입니다.");
            return "board/deleted";
        }

        BoardDTO boardDTO = boardOpt.get();
        model.addAttribute("dto", boardDTO);

        // ✅ 디버깅용 로그
        log.info("게시글 bno={} fileNames={}", bno, boardDTO.getFileNames());

        // 파라미터 없으면 DTO 값으로 대체
        if (boardType == null) {
            boardType = boardDTO.getBoardType();
        }

        // ---- 조회수 증가 로직 추가 ----
        if(boardType == BoardType.EVENT){
            // 이벤트는 단순 증가
            boardService.incrementViewCount(bno, null, null);
        } else {
            // NOTICE, FAQ 등 기존 로직
            Member member = rq.getMemberOrNull(); // 로그인 안 했으면 null 반환
            String cookieId = null;

            if(member != null){
                if(!boardService.hasViewed(bno, member.getMno(), null)){
                    boardService.incrementViewCount(bno, member.getMno(), null);
                }
            } else {
                Cookie[] cookies = request.getCookies();
                if(cookies != null){
                    for(Cookie c : cookies){
                        if(c.getName().equals("viewed_board")) cookieId = c.getValue();
                    }
                }
                if(cookieId == null){
                    cookieId = java.util.UUID.randomUUID().toString();
                    Cookie newCookie = new Cookie("viewed_board", cookieId);
                    newCookie.setMaxAge(60*60*24);
                    response.addCookie(newCookie);
                }

                if(!boardService.hasViewed(bno, null, cookieId)){
                    boardService.incrementViewCount(bno, null, cookieId);
                }
            }
        }
        // ---- 조회수 증가 끝 ----

        return switch(boardType) {
            case FAQ -> "board/faq/read";
            case EVENT -> "board/event/read";
            default -> "board/board/read";
        };
    }

    /** 관리자 게시글 수정 페이지 이동 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/modify")
    public String modifyForm(Long bno, PageRequestDTO pageRequestDTO, Model model){
        BoardDTO boardDTO = boardService.readOne(bno);
        model.addAttribute("boardDTO", boardDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        model.addAttribute("boardTypes", BoardType.values());
        return "board/board/modify";
    }

    /** 관리자 게시글 수정 처리 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/modify")
    public String modify(PageRequestDTO pageRequestDTO,
                         @Valid BoardDTO boardDTO,
                         BindingResult bindingResult,
                         @RequestParam(value="files", required=false) List<MultipartFile> newFiles,
                         @RequestParam(value="deleteFiles", required=false) List<String> deleteFiles,
                         RedirectAttributes redirectAttributes) {

        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addAttribute("bno", boardDTO.getBno());
            return "redirect:/board/modify?" + pageRequestDTO.getLink();
        }

        Member member = rq.mustGetMember();

        // EVENT 종료 여부 계산
        if(boardDTO.getBoardType() == BoardType.EVENT && boardDTO.getEndDate() != null){
            boardDTO.setExpired(boardDTO.getEndDate().isBefore(java.time.LocalDate.now()));
        }

        // 실제 수정 처리
        boardService.modify(boardDTO, member, newFiles, deleteFiles);

        redirectAttributes.addFlashAttribute("result", "modified");
        redirectAttributes.addAttribute("bno", boardDTO.getBno());
        return "redirect:/board/read?bno=" + boardDTO.getBno() + "&boardType=" + boardDTO.getBoardType();
    }

    /** 관리자 게시글 삭제 처리 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/remove")
    public String remove(BoardDTO boardDTO,
                         RedirectAttributes redirectAttributes) {

        Member member = rq.mustGetMember();

        BoardDTO dto = boardService.readOne(boardDTO.getBno());

        boardService.remove(dto.getBno(), member);

        List<String> fileNames = dto.getFileNames();
        if(fileNames != null && !fileNames.isEmpty()) removeFiles(fileNames);

        redirectAttributes.addFlashAttribute("result", "removed");
        return "redirect:/board/list?boardType=" + dto.getBoardType();
    }

    private void removeFiles(List<String> files) {
        for (String fileName : files) {
            try {
                Resource resource = new FileSystemResource(Paths.get(uploadPath, fileName));
                String contentType = Files.probeContentType(resource.getFile().toPath());
                resource.getFile().delete();

                if (contentType != null && contentType.startsWith("image")) {
                    File thumbnail = new File(uploadPath + File.separator + "s_" + fileName);
                    if (thumbnail.exists()) thumbnail.delete();
                }
            } catch (Exception e) {
                log.error("파일 삭제 오류: {}", e.getMessage());
            }
        }
    }

    /*@GetMapping("/uploads")
    @ResponseBody
    public ResponseEntity<Resource> getUpload(@RequestParam String file) throws Exception {
        // file이 절대 경로로 들어오는 것을 방지
        if(file.startsWith("/")) file = file.substring(1);

        Path path = Paths.get(uploadPath, file);
        Resource resource = new FileSystemResource(path);

        if (!resource.exists()) {
            // default 이미지
            path = Paths.get(uploadPath, "default/default.jpg");
            resource = new FileSystemResource(path);
            if(!resource.exists()) return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }*/
}
