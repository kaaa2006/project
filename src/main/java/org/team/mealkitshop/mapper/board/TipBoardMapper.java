package org.team.mealkitshop.mapper.board;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.team.mealkitshop.common.BoardReactionType;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.board.TipBoardDTO;
import org.team.mealkitshop.dto.board.TipReplyDTO;
import org.team.mealkitshop.service.member.MemberService;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class TipBoardMapper {
    private final MemberService memberService;

    public TipBoardMapper(MemberService memberService) {
        this.memberService = memberService;
    }
    // TipBoard ↔ TipBoardDTO 변환 담당 Mapper
    // DTO는 데이터 전달 + validation만 수행
    // Entity ↔ DTO 변환 로직을 Mapper로 분리
    // ModelMapper로 자동 변환

    // Entity → DTO 변환 (로그인 사용자 ID 없을 때)
    public TipBoardDTO toDTO(TipBoard entity) {

        return TipBoardDTO.fromEntity(entity, null); // 로그인 ID 필요 없으면 null
    }

    // Entity → DTO 변환 (로그인 사용자 ID 포함)
    public TipBoardDTO toDTO(TipBoard entity, String memberId) {

        return TipBoardDTO.fromEntity(entity, memberId);
    }

    // DTO → Entity 변환
    public TipBoard toEntity(TipBoardDTO dto) {
        TipBoard entity = new TipBoard();
        entity.setBno(dto.getBno());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setWriter(dto.getWriter());
        entity.setWriterId(dto.getWriterId());
        entity.setViewCount(dto.getViewCount());
        entity.setLikeCount(dto.getLikeCount());
        entity.setDislikeCount(dto.getDislikeCount());
        // topHelpful는 boolean → Integer 변환
        entity.setTopHelpful(dto.isTopHelpful() ? 1 : 0);
        return entity;
    }

}
