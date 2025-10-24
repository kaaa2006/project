package org.team.mealkitshop.service.board;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.team.mealkitshop.dto.board.BoardDTO;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;

@SpringBootTest
@Log4j2
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Test
    public void testRegister() {
        log.info("등록용 테스트 서비스 실행중...");
        log.info(boardService.getClass().getName());

        BoardDTO boardDTO = BoardDTO.builder()
                .title("서비스에서 만든 제목")
                .content("서비스에서 만든 내용")
                .writer("서비스님")
                .secretBoard(false)
                .secretPassword(null)
                .build();

        Long bno = boardService.register(boardDTO);

        log.info("테스트 결과 bno: " + bno);
    }

    @Test
    public void testModify() {
        BoardDTO boardDTO = BoardDTO.builder()
                .bno(1L)
                .title("서비스에서 수정된 제목")
                .content("서비스에서 수정된 내용")
                .build();

        boardService.modify(boardDTO); // 프론트에서 객체가 넘어가 수정되었는지 테스트

    }

    @Test
    public void testList() {
        // 프론트에서 넘어오는 데이터를 이용해서 페이징과 검색과 정렬 처리용
        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .type("tcw") // 제목, 내용, 작성자
                .keyword("1") // 1을 찾는다.
                .page(1) // 현재 페이지는 1
                .size(10) // 10개씩 보여달라
                .build();

        PageResponseDTO<BoardDTO> PageResponseDTO = boardService.list(pageRequestDTO);

        log.info(PageResponseDTO);

    }
}