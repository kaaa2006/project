package org.team.mealkitshop.service.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface BoardService {

    /** 게시글 등록 (로그인한 관리자(Member)만 가능) */
    Long register(BoardDTO boardDTO, Member member, List<MultipartFile> files);

    /** 게시글 단건 조회 (첨부파일 포함) */
    BoardDTO readOne(Long bno);

    /** 관리자 게시글 수정 (작성자 본인만 수정 가능, 제목/내용/첨부파일 수정 가능) */
    void modify(BoardDTO boardDTO, Member member, List<MultipartFile> newFiles, List<String> deleteFiles);

    /** 관리자 게시글 삭제 (작성자 본인만 가능) */
    void remove(Long bno, Member member);

    /** 게시판 타입별 페이징 리스트 조회 (status 필터링 포함) */
    PageResponseDTO<BoardDTO> listByType(BoardType boardType, PageRequestDTO pageRequestDTO, String status);

    /** 페이징 + 첨부파일 포함 게시글 리스트 조회 */
    PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO);

    /** 관리자 게시글 첨부파일 저장 */
    void saveFiles(Long bno, List<MultipartFile> files);

    Optional<BoardDTO> readOptional(Long bno);

    boolean hasViewed(Long boardId, Long memberId, String cookieId);
    void incrementViewCount(Long boardId, Long memberId, String cookieId);
}