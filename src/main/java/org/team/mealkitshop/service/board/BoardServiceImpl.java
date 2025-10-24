package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.common.BoardType;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardView;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.*;
import org.team.mealkitshop.repository.board.BoardRepository;
import org.team.mealkitshop.repository.board.BoardViewRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("boardService")
@Log4j2
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;
    private final BoardImageService boardImageService;
    private final BoardViewRepository boardViewRepository;


    // 관리자 게시글 등록
    @Override
    public Long register(BoardDTO boardDTO, Member member, List<MultipartFile> files) {
        // 작성자 강제 설정 (관리자 이름)
        boardDTO.setWriter(member.getMemberName());

        Board board = dtoTOEntity(boardDTO);
        Board saved = boardRepository.save(board);

        // 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            saveFiles(saved.getBno(), files);
        }

        return saved.getBno();
    }

    // 게시글 단건 조회 첨부파일(이미지) 포함 조회
    @Override
    public BoardDTO readOne(Long bno) {
        Board board = boardRepository.findByIdWithImage(bno)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board); // DB 반영
        BoardDTO dto = entityTODTO(board);

        // 등록일과 수정일이 같으면 수정일 null 처리
        if (dto.getUpdateTime() != null && dto.getUpdateTime().equals(dto.getRegTime())) {
            dto.setUpdateTime(null);
        }

        return dto;
    }

    /**
     * 게시글 수정
     * - 작성자 본인만 수정 가능
     * - 제목, 내용 수정 가능
     * - 첨부파일은 모두 초기화 후 새로 추가하는 방식
     *
     * @param boardDTO 수정할 데이터 DTO
     * @param member 로그인 사용자
     */
    @Override
    public void modify(BoardDTO boardDTO, Member member, List<MultipartFile> newFiles, List<String> deleteFiles) {
        Board board = boardRepository.findById(boardDTO.getBno())
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        if (!board.getWriter().equals(member.getMemberName())) {
            throw new AccessDeniedException("본인 글만 수정 가능합니다.");
        }

        // 제목/내용 변경
        board.change(boardDTO.getTitle(), boardDTO.getContent());

        // 삭제 체크된 파일 제거
        if (deleteFiles != null && !deleteFiles.isEmpty()) {
            deleteFiles.forEach(fileName -> {
                board.removeImage(fileName);           // 엔티티에서 제거
                boardImageService.deleteFile(fileName); // DB + 실제 파일 삭제
            });
        }

        if (newFiles != null && !newFiles.isEmpty()) {
            List<MultipartFile> uploadedFiles = newFiles.stream()
                    .filter(f -> f != null && !f.isEmpty()) // 실제 파일만 남기기
                    .collect(Collectors.toList());

            if (!uploadedFiles.isEmpty()) {
                List<String> savedFiles = boardImageService.saveImages(board.getBno(), uploadedFiles);
                savedFiles.forEach(board::addImage);
            }
        }

        boardRepository.save(board);
    }

    /**
     * 게시글 삭제
     * - 작성자 본인만 삭제 가능
     *
     * @param bno 삭제할 게시글 번호
     * @param member 로그인 사용자
     */
    @Override
    public void remove(Long bno, Member member) {
        Board board = boardRepository.findById(bno)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        // 권한 확인 (작성자 본인만)
        if (!board.getWriter().equals(member.getMemberName())) {
            throw new AccessDeniedException("본인 글만 삭제 가능합니다.");
        }

        boardRepository.delete(board);
    }

    @Override
    public boolean hasViewed(Long boardId, Long memberId, String cookieId){
        if(memberId != null){
            return boardViewRepository.existsByBoardIdAndMemberId(boardId, memberId);
        } else {
            return boardViewRepository.existsByBoardIdAndCookieId(boardId, cookieId);
        }
    }

    @Override
    public void incrementViewCount(Long boardId, Long memberId, String cookieId){
        Board board = boardRepository.findById(boardId).orElseThrow();
        board.setViewCount(board.getViewCount() + 1);
        boardRepository.save(board);

        BoardView view = BoardView.builder()
                .boardId(boardId)
                .memberId(memberId)
                .cookieId(cookieId)
                .build();
        boardViewRepository.save(view);
    }

    @Override
    public PageResponseDTO<BoardDTO> listByType(BoardType boardType, PageRequestDTO pageRequestDTO, String status) {

        Pageable pageable = pageRequestDTO.getPageable("bno");
        Page<Board> page;

        if(boardType == BoardType.EVENT) {
            if("expired".equals(status)) {
                page = boardRepository.findByBoardTypeAndEndDateBefore(boardType, LocalDateTime.now(), pageable);
            } else { // 진행중
                page = boardRepository.findOngoingEvents(boardType, LocalDateTime.now(), pageable);
            }
        } else {
            page = boardRepository.findByBoardType(boardType, pageable);
        }

        List<BoardDTO> dtos = page.getContent().stream()
                .map(this::entityTODTO)
                .toList();

        return PageResponseDTO.<BoardDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtos)
                .total((int) page.getTotalElements())
                .build();
    }

    /**
     * 게시글 리스트 조회 (페이징 + 첨부파일 포함)
     *
     * @param pageRequestDTO 요청 파라미터
     * @return PageResponseDTO<BoardListAllDTO>
     */
    @Override
    public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
        Pageable pageable = pageRequestDTO.getPageable("bno");
        Page<BoardListAllDTO> result = boardRepository.searchWithAll(
                pageRequestDTO.getTypes(),
                pageRequestDTO.getKeyword(),
                pageable
        );

        return PageResponseDTO.<BoardListAllDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(result.getContent())
                .total((int) result.getTotalElements())
                .build();
    }

    // ----------------------------
    // 관리자 첨부파일 저장
    // ----------------------------
    @Override
    public void saveFiles(Long bno, List<MultipartFile> files) {
        // 실제 파일 저장 로직 구현 (업로드 경로 + DB 등록)
        // TODO: 저장 경로 설정, DB 연동
        // 예: boardRepository.saveImage(bno, fileName);

        if (files != null && !files.isEmpty()) {
            boardImageService.saveImages(bno, files);
        }

    }

    // ----------------------------
    // DTO ↔ Entity 변환
    // ----------------------------
    private Board dtoTOEntity(BoardDTO dto) {
        Board board = modelMapper.map(dto, Board.class);

        // ★ 날짜 변환 수동 처리
        if(dto.getStartDate() != null) board.setStartDate(dto.getStartDate().atStartOfDay());
        if(dto.getEndDate() != null) board.setEndDate(dto.getEndDate().atTime(23,59,59));

        // ★ 이벤트 게시글 active 상태 설정
        if(board.getBoardType() == BoardType.EVENT) {
            boolean active = board.getEndDate() == null || board.getEndDate().isAfter(LocalDateTime.now());
            board.setActive(active);
            dto.setExpired(!active); // DTO 필드 세팅
        }

        return board;
    }

    private BoardDTO entityTODTO(Board board) {
        return BoardDTO.fromEntity(board); // 이렇게 바꾸기
    }

    @Override
    public Optional<BoardDTO> readOptional(Long bno) {
        return boardRepository.findById(bno)
                .map(BoardDTO::fromEntity); // 삭제 여부도 포함 가능
    }

}