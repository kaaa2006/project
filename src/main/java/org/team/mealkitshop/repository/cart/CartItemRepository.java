package org.team.mealkitshop.repository.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 장바구니 항목(CartItem)을 처리하는 레포지토리
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * 장바구니에 같은 상품이 이미 담겨 있는지 확인
     */
    Optional<CartItem> findByCartAndItem(Cart cart, Item item);

    /**
     * 장바구니에 담긴 모든 항목 조회
     */
    List<CartItem> findAllByCart(Cart cart);

    /**
     * 장바구니 안에서 특정 ID 목록에 해당하는 항목들만 조회
     * (본인 장바구니인지 확인 용도로 사용)
     */
    List<CartItem> findAllByCartAndCartItemIdIn(Cart cart, Collection<Long> cartItemIds);

    /**
     * 장바구니 안에서 특정 항목 1개를 조회 (본인 장바구니인지 검증)
     */
    Optional<CartItem> findByCartAndCartItemId(Cart cart, Long cartItemId);

    /**
     * 체크된 항목만 조회 (결제 대상용)
     */
    List<CartItem> findAllByCartAndCheckedTrue(Cart cart);

    /**
     * 여러 개의 장바구니 항목을 한 번에 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem ci where ci.cart = :cart and ci.cartItemId in :ids")
    int deleteAllByCartAndCartItemIdIn(Cart cart, Collection<Long> ids);

    // ✅ 회원 ID로 장바구니 개수 조회
    int countByCart_Member_Mno(Long mno); // ✅ 중간에 cart로 접근


    List<CartItem> findByCartAndCartItemIdIn(Cart cart, Collection<Long> cartItemIds);

    /*상품에서 추가 상품 삭제 시 장바구니에 있던 상품 항목도 삭제*/
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem ci where ci.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
