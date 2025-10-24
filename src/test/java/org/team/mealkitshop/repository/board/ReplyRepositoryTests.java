package org.team.mealkitshop.repository.board;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.Reply;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
class ReplyRepositoryTests {

    @Autowired  // 필드 선언
    private ReplyRepository replyRepository;

    @Test
    public void testInsert(){
        // 댓글 등록용 테스트
        Long bno = 2L; // db에 있는지 확인필요

        // 10번 게시물에 댓글을 넣어보자.
        Board board = Board.builder().bno(bno).build();
        // Board 게시물에 10번을 가져옴.

        Reply reply = Reply.builder()
                .board(board)   // bno
                .ReplyText("리포지토리에서 테스트")
                .Replyer("리포지토리")
                .build();
        // 10번게시물에 댓글 객체 생성

        replyRepository.save(reply); // insert into

    }


    @Test
    @Transactional  //import org.springframework.transaction.annotation.Transactional;
    public void testBoardReplies(){
        Long bno = 2L;  // 10게시물에 댓글을 가져와!!!

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rno").descending());
        //                  페이징처리요청용 0페이지, 10개 리스트 , rno 내림차순정렬

        Page<Reply> result = replyRepository.listOfBoard(bno, pageable);
        // JPQL를 이용한 selecte 처리용 코드
        // @Query("select r from Reply r where r.board.bno = :bno ")

        result.getContent().forEach(reply -> {
            log.info(reply);
        });

    }
}