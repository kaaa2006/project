package org.team.mealkitshop.repository.board;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardImage;
import org.team.mealkitshop.dto.board.BoardListReplyCountDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@SpringBootTest
@Log4j2
public class BoardRepositoryTests {

    @Autowired // 생성자 자동 주입
    private BoardRepository boardRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Test
    public void testInsert(){
        // db에 제약조건 실수로 제거하고 테스트 돌렸는데 테스트 돌아갔습니다
        // 제약조건 없이 만들어보고 나중에 추가할게요
        // secret_board 컬럼 varchar(50)로 변경함
        // 비밀게시글 Y/N 둘 다 정상 작동

        IntStream.rangeClosed(1,2).forEach(i -> {

            Board board = Board.builder()
                    .title("제목" + i)
                    .content("내용" + i)
                    .writer("user"+(i%10))
                    .secretBoard(true)
                    .secretPassword("1234")
                    .build();

            Board result = boardRepository.save(board);

            log.info("게시물 번호 출력 : " + result.getBno() + "게시물의 제목 : " + result.getTitle());

        });

    } // 게시글 더미데이터 주입 메서드 종료

    @Test
    public void testInsertAll(){ // 정상작동함
        // 모든 게시글 불러오기

        for(int i=0; i <=100; i++){
            Board board = Board.builder()
                    .title("제목" + i)
                    .content("내용" + i)
                    .writer("user"+(i%10))
                    .secretBoard(false)
                    .secretPassword(null)
                    .build();

            for (int j=0; j < 3; j++){
                if(i % 5 == 0){
                    continue;
                }
                board.addImage(UUID.randomUUID().toString(), i+"file"+j+".jpg");
            }// 첨부파일 더미데이터 for문 종료
            boardRepository.save(board);
        }// 게시물 더미데이터 for문 종료
    }// 모든 게시글 불러오기 메서드 종료

    @Test
    public void testUpdate(){
        // 게시글 수정
        // 정상 작동

        Long bno = 22L; // 22번 게시물을 가져와서 수정 후 테스트 종료

        Optional<Board> result = boardRepository.findById(bno); // bno를 찾아서 result에 넣는다.

        Board board = result.orElseThrow(); // 가져온 값이 있으면 board 타입에 객체에 넣는다.

        board.change("팀 프로젝트 수정테스트 제목", "팀 프로젝트 수정테스트 내용");
        // 제목과 내용만 수정할 수 있는 메서드

        boardRepository.save(board);
    }

    @Test
    public void testDelete(){
        // 게시글 삭제
        // 정상 작동

        Long bno = 22L;

        boardRepository.deleteById(bno);
    }


    @Test
    public void testSelect(){ 
        // 게시글 하나만 보기
        // 정상 작동

        Long bno = 10L; // 게시물 번호가 22인 개체 확인

        Board board = boardRepository.findByIdWithImage(bno).orElseThrow(); // 새로 추가된 코드
        // ImageSet가 한번에 로딩됨
        // 세션이 닫혀도 컬렉션 접근이 가능
        
//        Optional<Board> result = boardRepository.findById(bno);
//        // Optional 널값이 나올 경우를 대비한 객첸
//
//        Board board = result.orElseThrow(); // 값이 있으면 넣어라

        log.info(bno + "가 데이터 베이스에 존재합니다.");
        log.info(board);

    }

    @Test
    @Transactional // 추가
    public void testSearchAll(){
        // 프론트에서 t가 선택되면 title, c가 선택되면 content, w가 선택되면 writer가 조건으로 제시됨
        // 정상작동

        String[] types = {"t", "w"};  // 검색 조건

        String keyword = "10";  // 검색 단어

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<Board> result = boardRepository.searchAll(types, keyword, pageable);

        log.info("전체 게시물 수 : " + result.getTotalElements());
        log.info("총 페이시 수 : " + result.getTotalPages());
        log.info("현재 페이지 번호 : " + result.getNumber());
        log.info("페이지당 데이터 개수 : " + result.getSize());
        log.info("다음페이지 여부 : " + result.hasNext());
        log.info("시작페이지 여부 : " + result.isFirst());

        result.getContent().forEach(board -> log.info(board));
    }

    @Test
    @Transactional // 추가
    public void testPaging(){
        // 정상 작동

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<Board> result = boardRepository.findAll(pageable);

        log.info("전체 게시물 수 : " + result.getTotalElements());
        log.info("총 페이시 수 : " + result.getTotalPages());
        log.info("현재 페이지 번호 : " + result.getNumber());
        log.info("페이지당 데이터 개수 : " + result.getSize());
        log.info("다음페이지 여부 : " + result.hasNext());
        log.info("시작페이지 여부 : " + result.isFirst());

        // 콘솔에 결과 출력
        List<Board> boardList = result.getContent(); // 페이징처리된 내용을 가져와라

        boardList.forEach(board -> log.info(board));

    }

    @Test
    public void testSearch1(){ 
        // 정상 작동

        Pageable pageable = PageRequest.of(1,10, Sort.by("bno").descending());

        Page<Board> result = boardRepository.search1(pageable); // 페이징 기법을 사용하여 title = 1 값을 찾아옴

        result.getContent().forEach(board -> log.info(board));

    }

    @Test
    public void testSearchReplyCount(){
        // 정상작동

        String[] types = {"t", "c", "w"}; // 제목, 내용, 작성자
        String keyword = "1"; // 제목이나 내용이나 작성자에 1값을 찾는다.

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<BoardListReplyCountDTO> result = boardRepository.searchWithReplyCount(types, keyword, pageable);

        log.info("전체 게시물 수 : " + result.getTotalElements());
        log.info("총 페이시 수 : " + result.getTotalPages());
        log.info("현재 페이지 번호 : " + result.getNumber());
        log.info("페이지당 데이터 개수 : " + result.getSize());
        log.info("다음페이지 여부 : " + result.hasNext());
        log.info("시작페이지 여부 : " + result.isFirst());

        result.getContent().forEach(board -> log.info(board));

    }

    @Test
    @Transactional
    @Commit
    public void testRemoveAll(){
    // 정상작동
        
        Long bno = 10L; // 10번 게시물을 삭제하면 댓글과 첨부파일이 모두 삭제되어야 함

        replyRepository.deleteByBoard_Bno(bno); // 자식부터 삭제
        boardRepository.deleteById(bno);        // 부모 삭제

    }

    @Test
    public void testInsertWithImage(){
        // 정상 작동

        Board board = Board.builder()
                .title("팀 프로젝트 이미지 테스트")
                .content("팀 프로젝트 첨부파일 테스트")
                .writer("tester")
                .secretBoard(false)
                .secretPassword(null)
                .build();

        for(int i=0; i<3; i++){
            board.addImage(UUID.randomUUID().toString(), "file"+i+".jpg");
        }
        boardRepository.save(board);

    }

    @Test
    @Transactional
    public void testReadWithImage(){ 
        // 정상작동
        
        Optional<Board> result = boardRepository.findById(20L);
        // board 테이블에 10번 게시물을 가져와라
        // 게시물이 없으면 오류 발생
        
        Board board = result.orElseThrow();
        // 예외가 없으면 board 객체에 담는다.
        
        log.info(board); // 게시물 객체
        log.info("-------------------------------------------");
        log.info(board.getImageSet()); // 첨부파일 객체
        
    }

    @Test
    public void testReadWithImagesEntityGraph(){
        // 정상 작동
        Optional<Board> result = boardRepository.findByIdWithImage(11L);

        Board board = result.orElseThrow();
        log.info(board);
        log.info("--------------------");
        for (BoardImage boardImage : board.getImageSet()) {
            log.info(boardImage);
        }

    }

    @Transactional
    @Commit
    @Test
    public void testModifyImages(){
        // 정상 작동

        Optional<Board> result = boardRepository.findByIdWithImage(11L);
        Board board = result.orElseThrow();

        board.clearImage(); // board 테이블에 연결된 Image 테이블을 전체 삭제

        for(int i=0; i<2; i++){
            // 전에는 3개의 첨부지만 2로 수정 하려 함
            board.addImage(UUID.randomUUID().toString(), "updatefile"+i+".jpg");
        }
        boardRepository.save(board);

    }

    @Test // N+1 오류 발생 테스트
    @Transactional
    public void testSearchImageReply(){
        // 리스트 페이지에서 댓글 수와 게시물 목록 이미지가 처리되는 부분
        // 정상 작동

        Pageable pageable = PageRequest.of(1,10, Sort.by("bno").descending());
        boardRepository.searchWithAll(null, null, pageable);

    }


}
