package org.team.mealkitshop.repository.address;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /* ===== 조회 (화면 용) ===== */
    List<Address> findAllByMember_MnoOrderByIsDefaultDescAddressIdAsc(Long mno);
    List<Address> findByMemberOrderByIsDefaultDescAddressIdAsc(Member member);

    /* (선택) mno만 알고 있을 때도 편하게 쓰려고 추가 */
    List<Address> findByMember_MnoOrderByIsDefaultDescAddressIdAsc(Long mno);

    /* ===== 기본 배송지 조회/존재 ===== */
    Optional<Address> findFirstByMember_MnoAndIsDefaultTrue(Long mno);
    boolean existsByMember_MnoAndIsDefaultTrue(Long mno);
    long countByMember_MnoAndIsDefaultTrue(Long mno);

    /* ===== 기본 배송지 갱신 유틸 ===== */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Address a
              set a.isDefault = false
            where a.member.mno = :mno
              and a.addressId <> :keepId
              and a.isDefault = true
           """)
    int clearDefaultExcept(@Param("mno") Long mno, @Param("keepId") Long keepId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Address a
              set a.isDefault = false
            where a.member.mno = :mno
              and a.isDefault = true
           """)
    int clearAllDefault(@Param("mno") Long mno);

    /* ===== 소유자 기반 단건 조회(수정/삭제 시 권한검사) ===== */
    Optional<Address> findByAddressIdAndMember_Mno(Long addressId, Long mno);

    /* ===== 소유자 기반 삭제 ===== */
    // 파생 쿼리로 안전하게 삭제(영향받은 row 수 반환)
    int deleteByAddressIdAndMember_Mno(Long addressId, Long mno);

    List<Address> findByMember(Member member);

    /* ===== 선택: 기본 배송지 개수/아이디 모음 등 필요시 ===== */
    // List<Long> findAddressIdByMember_Mno(Long mno);  // 필요시 주석 해제해 사용
}