package org.team.mealkitshop.mapper.board;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Component;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.dto.board.ReviewBoardDTO;

@Component
public class ReviewBoardMapper {

    private final ModelMapper modelMapper;

    public ReviewBoardMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    // Entity -> DTO (기존대로 replies 스킵)
    public ReviewBoardDTO toDTO(ReviewBoard entity) {
        TypeMap<ReviewBoard, ReviewBoardDTO> tm =
                modelMapper.getTypeMap(ReviewBoard.class, ReviewBoardDTO.class);
        if (tm == null) {
            tm = modelMapper.createTypeMap(ReviewBoard.class, ReviewBoardDTO.class)
                    .addMappings(m -> m.skip(ReviewBoardDTO::setReplies));
        }
        ReviewBoardDTO dto = tm.map(entity);
        dto.setHelpfulCount(entity.getHelpfulCount());
        dto.setNotHelpfulCount(entity.getNotHelpfulCount());
        return dto;
    }

    // DTO -> Entity ( writerMember, replies 매핑 스킵)
    public ReviewBoard toEntity(ReviewBoardDTO dto) {
        if (dto == null) return null;

        // 신규 등록 관점에서 꼭 필요한 필드만 옮깁니다.
        ReviewBoard.ReviewBoardBuilder builder = ReviewBoard.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(dto.getWriter())
                .secretBoard(dto.isSecretBoard())
                .secretPassword(dto.getSecretPassword());

        // 필요 시 초기 카운트/뷰값 반영 (없어도 무방)
        if (dto.getHelpfulCount() > 0) builder.helpfulCount(dto.getHelpfulCount());
        if (dto.getNotHelpfulCount() > 0) builder.notHelpfulCount(dto.getNotHelpfulCount());
        if (dto.getViewCount() > 0) builder.viewCount(dto.getViewCount());

        return builder.build();
    }
}
