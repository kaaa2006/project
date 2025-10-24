package org.team.mealkitshop.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;

    /** 현재 로그인한 사용자의 상세 정보 조회 (폼/소셜 공통) */
    public MemberDetailDTO getMyInfo() {
        String email = normalize(extractCurrentEmail());
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("회원이 존재하지 않습니다: " + email));

        List<Address> addresses = addressRepository.findByMemberOrderByIsDefaultDescAddressIdAsc(m);

        return MemberDetailDTO.builder()
                .mno(m.getMno())
                .email(m.getEmail())
                .memberName(m.getMemberName())
                .phone(m.getPhone())
                .grade(m.getGrade())
                .points(m.getPoints())
                .marketingYn(m.isMarketingYn())
                .addresses(addresses.stream().map(a -> {
                    MemberDetailDTO.AddressDTO dto = new MemberDetailDTO.AddressDTO();
                    dto.setId(a.getAddressId());
                    dto.setAlias(a.getAlias());
                    dto.setZipCode(a.getZipCode());
                    dto.setAddr1(a.getAddr1());
                    dto.setAddr2(a.getAddr2());
                    dto.setIsDefault(a.isDefault());
                    return dto;
                }).collect(Collectors.toList()))
                .build();
    }

    /** ✅ 기본(대표) 배송지 1건 반환: 있으면 기본주소, 없으면 첫 번째 주소, 모두 없으면 empty */
    public Optional<Address> findDefaultAddress(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("회원이 존재하지 않습니다: " + memberId));

        List<Address> list = addressRepository.findByMemberOrderByIsDefaultDescAddressIdAsc(m);
        return list.stream().filter(Address::isDefault).findFirst()
                .or(() -> list.stream().findFirst());
    }

    /** 현재 로그인 사용자의 email 추출 (폼/OAuth2 모두 대응) */
    private String extractCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object p = auth.getPrincipal();

        // 폼 로그인
        if (p instanceof UserDetails u) return u.getUsername();

        // 소셜 로그인
        if (p instanceof OAuth2User ou) {
            // 1) 표준 위치 시도
            String email = ou.getAttribute("email");
            if (email != null) return email;

            // 2) Kakao: kakao_account.email
            Map<String, Object> kakao = ou.getAttribute("kakao_account");
            if (kakao != null && kakao.get("email") instanceof String kEmail) {
                return kEmail;
            }

            // 3) Naver: response.email
            Map<String, Object> naver = ou.getAttribute("response");
            if (naver != null && naver.get("email") instanceof String nEmail) {
                return nEmail;
            }
        }

        // 폴백
        return auth.getName();
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}