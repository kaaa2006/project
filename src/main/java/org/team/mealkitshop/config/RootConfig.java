package org.team.mealkitshop.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.board.TipBoard;
import org.team.mealkitshop.domain.board.TipReply;
import org.team.mealkitshop.dto.board.ReviewBoardDTO;
import org.team.mealkitshop.dto.board.TipBoardDTO;
import org.team.mealkitshop.dto.board.TipReplyDTO;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Configuration // 환경설정용 이라고 스프링에게 알려준다.
public class RootConfig {

    @Bean // 환경설정용 객체로 지정
    public ModelMapper getMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.LOOSE);

        // ReviewBoard → ReviewBoardDTO
        modelMapper.typeMap(ReviewBoard.class, ReviewBoardDTO.class)
                .addMappings(mapper -> mapper.map(
                        src -> src
                                .getReplies() != null ? new ArrayList<>(src.getReplies()) : new ArrayList<>(),
                        ReviewBoardDTO::setReplies
                ));

        // TipBoard → TipBoardDTO
        modelMapper.typeMap(TipBoard.class, TipBoardDTO.class)
                .addMappings(mapper -> mapper.skip(TipBoardDTO::setViewCount))
                .addMappings(mapper -> mapper.skip(TipBoardDTO::setReplies)); // replies는 수동 처리

        // TipReply → TipReplyDTO는 fromEntity에서 안전하게 처리하므로 별도 매핑 불필요

        return modelMapper;
    }
}