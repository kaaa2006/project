package org.team.mealkitshop.repository.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.team.mealkitshop.domain.cart.Cart;

import java.util.Optional;

/**
 * 장바구니(Cart) 레포지토리
 * - 회원 기준으로 장바구니를 조회하거나 생성 여부를 확인할 수 있음
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 회원 번호(mno)로 장바구니 조회
     * - 장바구니(cart) + 항목들(items) + 각 항목의 상품(item)까지 한 번에 로딩
     * - 화면에서 N+1 문제를 막기 위해 EntityGraph 사용
     */
    @EntityGraph(attributePaths = {"items", "items.item"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<Cart> findByMember_Mno(Long mno);

    /**
     * 회원 번호로 장바구니 존재 여부 확인
     * - 장바구니가 없으면 생성하는 로직에서 사용
     */
    boolean existsByMember_Mno(Long mno);
}
