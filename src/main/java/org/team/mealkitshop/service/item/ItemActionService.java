package org.team.mealkitshop.service.item;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemLike;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.exception.OutOfStockException;
import org.team.mealkitshop.repository.item.ItemLikeRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemActionService {

    private final ItemRepository itemRepository;
    private final ItemLikeRepository itemLikeRepository;
    private final MemberRepository memberRepository;

    /** 상세 진입 시 조회수 +1 (원자 업데이트) */
    public void increaseViewCount(Long itemId) {
        if (itemRepository.incrementView(itemId) == 0) {
            throw new EntityNotFoundException("Item not found: " + itemId);
        }
    }

    /** 주문 시 재고 차감(음수 방지). 실패 시 예외 */
    public void decreaseStockOrThrow(Long itemId, int qty) {
        if (qty < 1) throw new IllegalArgumentException("qty must be >= 1");
        if (itemRepository.decreaseStockSafely(itemId, qty) == 0) {
            // 기존: IllegalStateException -> 통일: OutOfStockException
            throw new OutOfStockException("재고 부족 또는 상품 없음 (id=" + itemId + ", qty=" + qty + ")");
        }
    }

    /** 좋아요 토글 (true=좋아요 설정, false=해제) — 회원 PK: mno */
    public boolean toggleLike(Long itemId, Long memberMno) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        return itemLikeRepository.findByMember_MnoAndItem_Id(memberMno, itemId)
                .map(existing -> {
                    itemLikeRepository.delete(existing);
                    itemRepository.decrementLikeSafely(itemId); // where itemLike > 0
                    return false;
                })
                .orElseGet(() -> {
                    try {
                        Member memberRef = memberRepository.getReferenceById(memberMno);
                        itemLikeRepository.save(ItemLike.builder().member(memberRef).item(item).build());
                        itemRepository.incrementLike(itemId);
                        return true;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 경쟁 삽입 보정: 이미 존재한다고 판단하고 해제 플로우로 전환하거나 무시
                        return true; // 이미 좋아요 상태
                    }
                });
    }

    /** 단건: 내가 좋아요 눌렀는지 */
    @Transactional(readOnly = true)
    public boolean isLiked(Long itemId, Long memberMno) {
        return itemLikeRepository.existsByMember_MnoAndItem_Id(memberMno, itemId);
    }

    /** 배치: 목록용 liked 표시 — 반환: 좋아요한 itemId 집합 */
    @Transactional(readOnly = true)
    public Set<Long> likedItemIds(Long memberMno, Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Set.of();
        List<Long> liked = itemLikeRepository.findLikedItemIds(memberMno, itemIds);
        return new HashSet<>(liked);
    }
}
