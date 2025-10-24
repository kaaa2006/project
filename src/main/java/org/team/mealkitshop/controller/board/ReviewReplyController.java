package org.team.mealkitshop.controller.board;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReviewReplyDTO;
import org.team.mealkitshop.service.board.ReviewReplyService;

import java.util.Map;

@RestController
@RequestMapping("/board/review/reply")
@RequiredArgsConstructor
@Log4j2
public class ReviewReplyController {

    private final ReviewReplyService reviewReplyService;

    private Map<String, Long> toResult(Long rno) {
        return Map.of("rno", rno);
    }

    // ========================
    // 댓글 등록 - 관리자만
    // ========================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> register(@Valid @RequestBody ReviewReplyDTO reviewReplyDTO,
                                      BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        Long rno = reviewReplyService.addReply(reviewReplyDTO);
        return toResult(rno);
    }

    // ========================
    // 댓글 목록 조회 - 모두 가능
    // ========================
    @GetMapping("/list/{bno}")
    public PageResponseDTO<ReviewReplyDTO> getList(@PathVariable Long bno,
                                                                  PageRequestDTO pageRequestDTO) {
        return reviewReplyService.getListOfBoard(bno, pageRequestDTO);
    }

    // ========================
    // 댓글 삭제 - 관리자만
    // ========================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{rno}")
    public Map<String, Long> remove(@PathVariable Long rno) {
        reviewReplyService.removeByAdmin(rno);
        return toResult(rno);
    }

    // ========================
    // 댓글 수정 - 관리자만
    // ========================
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{rno}")
    public Map<String, Long> modify(@PathVariable Long rno,
                                    @Valid @RequestBody ReviewReplyDTO reviewReplyDTO,
                                    BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        reviewReplyDTO.setRno(rno);
        reviewReplyService.modify(reviewReplyDTO);
        return toResult(rno);
    }
}