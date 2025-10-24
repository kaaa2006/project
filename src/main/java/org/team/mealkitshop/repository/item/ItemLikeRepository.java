package org.team.mealkitshop.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.team.mealkitshop.domain.item.ItemLike;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/* 추후 해당 기능 미사용 시 정리 예정 */
public interface ItemLikeRepository extends JpaRepository<ItemLike, Long> {

    // 파생쿼리: 연관경로 + 실제 PK명 사용 (Member.mno, Item.id)
    boolean existsByMember_MnoAndItem_Id(Long memberId, Long itemId);

    Optional<ItemLike> findByMember_MnoAndItem_Id(Long memberId, Long itemId);

    long deleteByItem_Id(Long itemId);  // 아이템 삭제 전 정리

    @Query("select il.item.id from ItemLike il " +
            "where il.member.mno = :memberId and il.item.id in :itemIds")
    List<Long> findLikedItemIds(@Param("memberId") Long memberId,
                                @Param("itemIds") Collection<Long> itemIds);
}
