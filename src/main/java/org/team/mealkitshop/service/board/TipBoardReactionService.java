package org.team.mealkitshop.service.board;

import org.team.mealkitshop.dto.board.TipBoardReactionDTO;

/**
 * 팁 게시판 좋아요/싫어요 전용 서비스 인터페이스
 * - 사용자 단위 토글 처리
 * - 현재 게시글 상태와 UI용 카운트를 반환
 * - 이미 눌렀는지 여부 확인 메서드 제공
 */
public interface TipBoardReactionService {

    /**
     * 좋아요 / 싫어요 토글 처리
     * - 사용자가 이미 클릭한 경우: 토글 해제
     * - 반대 반응이 클릭되어 있는 경우: 기존 반응 삭제 후 새 반응 추가
     * @param dto 클라이언트에서 전달한 TipBoardReactionDTO (게시글 ID, 사용자 ID, 반응 타입)
     * @return TipBoardReactionDTO - 최종 반응 상태 및 카운트 포함
     */
    TipBoardReactionDTO toggleReaction(TipBoardReactionDTO dto);

    /**
     * 특정 게시글에 사용자가 이미 좋아요 클릭했는지 확인
     * @param tipBoardId 게시글 ID
     * @param userId 사용자 ID
     * @return true이면 이미 좋아요 클릭됨
     */
    boolean isAlreadyAddGoodRp(long tipBoardId, String userId);

    /**
     * 특정 게시글에 사용자가 이미 싫어요 클릭했는지 확인
     * @param tipBoardId 게시글 ID
     * @param userId 사용자 ID
     * @return true이면 이미 싫어요 클릭됨
     */
    boolean isAlreadyAddBadRp(long tipBoardId, String userId);
}