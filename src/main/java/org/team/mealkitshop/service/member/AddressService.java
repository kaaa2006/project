package org.team.mealkitshop.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.address.AddressCreateDTO;
import org.team.mealkitshop.dto.address.AddressResponseDTO;
import org.team.mealkitshop.dto.address.AddressUpdateDTO;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;

    /* ===== Create ===== */
    @Transactional
    public Address create(Long memberId, AddressCreateDTO dto) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Address a = new Address();
        a.setMember(m);
        a.setAlias(nz(dto.getAlias()));
        a.setZipCode(nz(dto.getZipCode()));
        a.setAddr1(nz(dto.getAddr1()));
        a.setAddr2(nz(dto.getAddr2()));
        a.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));

        addressRepository.save(a);

        if (a.isDefault()) {
            addressRepository.clearDefaultExcept(memberId, a.getAddressId());
        }
        return a;
    }

    /* ===== Update ===== */
    @Transactional
    public Address update(Long memberId, AddressUpdateDTO dto) {
        Address a = addressRepository.findByAddressIdAndMember_Mno(dto.getId(), memberId)
                .orElseThrow(() -> new IllegalArgumentException("주소가 존재하지 않거나 권한이 없습니다."));

        if (dto.getAlias()    != null) a.setAlias(dto.getAlias());
        if (dto.getZipCode()  != null) a.setZipCode(dto.getZipCode());
        if (dto.getAddr1()    != null) a.setAddr1(dto.getAddr1());
        if (dto.getAddr2()    != null) a.setAddr2(dto.getAddr2());
        if (dto.getIsDefault()!= null) a.setDefault(dto.getIsDefault());

        addressRepository.save(a);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            addressRepository.clearDefaultExcept(memberId, a.getAddressId());
        }
        return a;
    }

    /* ===== Delete ===== */
    @Transactional
    public void delete(Long memberId, Long addressId) {
        Address a = addressRepository.findByAddressIdAndMember_Mno(addressId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("주소가 존재하지 않거나 권한이 없습니다."));

        boolean wasDefault = a.isDefault();
        addressRepository.delete(a);

        // 기본 주소를 지웠다면 남은 주소 중 첫 번째를 기본으로(선택 로직)
        if (wasDefault) {
            List<Address> rest = addressRepository.findByMember_MnoOrderByIsDefaultDescAddressIdAsc(memberId);
            if (!rest.isEmpty()) {
                Address first = rest.get(0);
                first.setDefault(true);
                addressRepository.save(first);
                addressRepository.clearDefaultExcept(memberId, first.getAddressId());
            }
        }
    }

    /* ===== Mapping ===== */
    public AddressResponseDTO toDto(Address a) {
        AddressResponseDTO d = new AddressResponseDTO();
        d.setId(a.getAddressId());
        d.setAlias(a.getAlias());
        d.setZipCode(a.getZipCode());
        d.setAddr1(a.getAddr1());
        d.setAddr2(a.getAddr2());
        d.setIsDefault(a.isDefault());
        return d;
    }

    private String nz(String s){ return s == null ? "" : s; }
}