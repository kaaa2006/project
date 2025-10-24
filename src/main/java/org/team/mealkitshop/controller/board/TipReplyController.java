package org.team.mealkitshop.controller.board;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.TipReplyDTO;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.service.board.TipReplyService;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TipReplyController
 * - TIP 게시글 댓글 CRUD
 * - 로그인 사용자 누구나 댓글 작성 가능
 * - 수정/삭제는 작성자 본인만 가능
 */
@RestController
@RequestMapping("/tip/replies")
@RequiredArgsConstructor
@Log4j2
public class TipReplyController {

    private final TipReplyService tipReplyService;
    private final MemberRepository memberRepository;

    // rno 반환용 공통 메서드
    // null 체크 포함
    private Map<String, Long> toResult(Long rno) {
        if (rno == null) return Collections.emptyMap();
        Map<String, Long> result = new HashMap<>();
        result.put("rno", rno);
        return result;
    }

    /** 댓글 등록 - 로그인 사용자 누구나 가능 */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TipReplyDTO> register(
            @Valid @RequestBody TipReplyDTO tipReplyDTO) {

        log.info("Register TipReplyDTO: {}", tipReplyDTO);

        // 회원 정보 가져오기
        Member member = memberRepository.findById(tipReplyDTO.getWriterId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        tipReplyDTO.setWriterName(member.getMemberName()); // 이름 세팅

        Long rno = tipReplyService.addReply(tipReplyDTO);
        tipReplyDTO.setRno(rno);

        return ResponseEntity.ok(tipReplyDTO); // writerName 포함 반환
    }

    /** 게시글별 댓글 목록 조회 - 페이징 적용 */
    @GetMapping("/list/{bno}")
    public ResponseEntity<PageResponseDTO<TipReplyDTO>> getList(
            @PathVariable("bno") Long bno,
            @ModelAttribute PageRequestDTO pageRequestDTO) {

        PageResponseDTO<TipReplyDTO> page = tipReplyService.getListOfBoard(bno, pageRequestDTO);
        return ResponseEntity.ok(page);
    }

    /** 댓글 단일 조회 */
    @GetMapping("/{rno}")
    public ResponseEntity<TipReplyDTO> read(@PathVariable Long rno) {
        TipReplyDTO dto = tipReplyService.read(rno);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    /** 댓글 수정 - 작성자 본인만 가능 */
    //@PreAuthorize("@tipReplyService.isOwner(#rno, authentication.name)")
    @PutMapping(value = "/{rno}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> modify(
            @PathVariable Long rno,
            @Valid @RequestBody TipReplyDTO tipReplyDTO,
            Authentication authentication) {

        String username = authentication.getName(); // 로그인 사용자 이메일
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        Long memberId = member.getMno();

        // 작성자 본인 체크
        if (!tipReplyService.isOwner(rno, username)) {
            return ResponseEntity.status(403).build(); // 권한 없음
        }

        tipReplyDTO.setWriterId(memberId); // 서버에서 채움
        tipReplyDTO.setRno(rno);           // URL PathVariable로 채움
        tipReplyService.modify(tipReplyDTO);

        return ResponseEntity.ok(toResult(rno));
    }

    /** 댓글 삭제 - 작성자 본인만 가능 */
    //@PreAuthorize("@tipReplyService.isOwner(#rno, principal)")
    @DeleteMapping("/{rno}")
    public ResponseEntity<Map<String, Long>> remove(@PathVariable Long rno, Authentication authentication) {
        String username = authentication.getName(); // 로그인한 사용자 username(email)

        // DB에서 Member 조회 후 ID 가져오기
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        Long memberId = member.getMno();

        tipReplyService.remove(rno, memberId); // 서비스는 기존대로 Long 사용
        log.info("Remove TipReply rno: {} by memberId: {}", rno, memberId);

        return ResponseEntity.ok(toResult(rno));
    }
}