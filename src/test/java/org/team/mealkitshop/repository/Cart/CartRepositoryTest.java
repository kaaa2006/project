package org.team.mealkitshop.repository.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartRepositoryTest {

    @Autowired
    CartRepository cartRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired TestEntityManager tem;

    @Test
    @Rollback(false)
    @DisplayName("member.mno로 Cart 조회 - 존재하는 경우")
    void findByMemberMno_exists() {
        Member member = memberRepository.save(
                Member.builder()
                        .email("aterwtada1231dad@example.com")
                        .password("pw33")
                        .memberName("uytitdad")
                        .phone("01000000033")
                        .role(Role.USER)
                        .status(Status.ACTIVE)
                        .provider(Provider.Local)
                        .grade(Grade.BASIC)
                        .marketingYn(false)
                        .points(0)
                        .build()
        );

        Cart cart = cartRepository.save(Cart.createFor(member));

        tem.flush();
        tem.clear();

        Long mno = member.getMno();
        Optional<Cart> found = cartRepository.findByMember_Mno(mno);

        assertThat(found).isPresent();
        assertThat(found.get().getCartId()).isEqualTo(cart.getCartId());
        assertThat(found.get().getMember().getMno()).isEqualTo(mno);
    }

    @Test
    @DisplayName("member.mno로 Cart 조회 - 없는 경우 Optional.empty")
    void findByMemberMno_notExists() {
        assertThat(cartRepository.findByMember_Mno(999L)).isEmpty();
    }
}
