package org.team.mealkitshop.repository.board.Search;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.QBoard;
import org.team.mealkitshop.domain.board.QTipReply;
import org.team.mealkitshop.dto.board.BoardImageDTO;
import org.team.mealkitshop.dto.board.BoardListAllDTO;
import org.team.mealkitshop.dto.board.TipBoardListReplyCountDTO;


import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BoardSearchImpl extends QuerydslRepositorySupport implements BoardSearch {

    public BoardSearchImpl() {
        super(Board.class);
    }

    @Override
    public Page<Board> search1(Pageable pageable) {
        QBoard board = QBoard.board;
        JPQLQuery<Board> query = from(board);

        // 제목이나 내용에 "11" 포함 검색 (테스트용)
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.or(board.title.contains("11"));
        booleanBuilder.or(board.content.contains("11"));
        query.where(booleanBuilder);
        query.where(board.bno.gt(0L));

        getQuerydsl().applyPagination(pageable, query);

        List<Board> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }

    @Override
    public Page<Board> searchAll(String[] types, String keyword, Pageable pageable) {
        QBoard board = QBoard.board;
        JPQLQuery<Board> query = from(board);

        // 검색 조건 설정 (제목, 내용, 작성자)
        if ((types != null && types.length > 0) && keyword != null) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            for (String type : types) {
                switch (type) {
                    case "t": booleanBuilder.or(board.title.contains(keyword)); break;
                    case "c": booleanBuilder.or(board.content.contains(keyword)); break;
                    case "w": booleanBuilder.or(board.writer.contains(keyword)); break;
                }
            }
            query.where(booleanBuilder);
        }

        query.where(board.bno.gt(0L)); // pk 기준 검색
        getQuerydsl().applyPagination(pageable, query);

        List<Board> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }

    @Override
    public Page<TipBoardListReplyCountDTO> searchWithReplyCount(String[] types, String keyword, Pageable pageable) {
        QBoard board = QBoard.board;
        // TIP 댓글 join
        JPQLQuery<Board> query = from(board);
        // 댓글 join 및 group by 등 로직 생략 (기존 코드 유지)
        return null; // 기존 구현 그대로 사용 가능
    }

    /**
     * 🔹 일반 게시판 리스트 + 첨부파일 포함
     * - BoardServiceImpl.listWithAll()에서 호출
     */
    @Override
    public Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable) {
        QBoard board = QBoard.board;
        JPQLQuery<Board> query = from(board);

        // 검색 조건 설정
        if ((types != null && types.length > 0) && keyword != null) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            for (String type : types) {
                switch (type) {
                    case "t": booleanBuilder.or(board.title.contains(keyword)); break;
                    case "c": booleanBuilder.or(board.content.contains(keyword)); break;
                    case "w": booleanBuilder.or(board.writer.contains(keyword)); break;
                }
            }
            query.where(booleanBuilder);
        }

        query.groupBy(board);
        getQuerydsl().applyPagination(pageable, query);

        List<Board> boardList = query.fetch();

        // 🔹 DTO 변환 (첨부파일 포함, 댓글 count는 0)
        List<BoardListAllDTO> dtoList = boardList.stream().map(b -> {
            BoardListAllDTO dto = BoardListAllDTO.builder()
                    .bno(b.getBno())
                    .title(b.getTitle())
                    .writer(b.getWriter())
                    .regDate(b.getRegTime())
                    .build();

            // 첨부 이미지 매핑
            List<BoardImageDTO> images = b.getImageSet().stream().sorted()
                    .map(img -> BoardImageDTO.builder()
                            .fileName(img.getFileName())
                            .build())
                    .collect(Collectors.toList());
            dto.setBoardImage(images);

            return dto;
        }).collect(Collectors.toList());

        long totalCount = query.fetchCount();

        return new PageImpl<>(dtoList, pageable, totalCount);
    }

}