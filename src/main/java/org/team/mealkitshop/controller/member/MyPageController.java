package org.team.mealkitshop.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.dto.address.AddressCreateDTO;
import org.team.mealkitshop.dto.address.AddressResponseDTO;
import org.team.mealkitshop.dto.address.AddressUpdateDTO;
import org.team.mealkitshop.dto.member.MemberDeleteDTO;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.dto.member.MemberUpdateDTO;
import org.team.mealkitshop.service.member.MemberQueryService;
import org.team.mealkitshop.service.member.MemberService;
import org.team.mealkitshop.service.member.MemberUpdateService;
import org.team.mealkitshop.service.member.AddressService;   // ✅ 추가

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MemberService memberService;             // 기존 조회용 (필요 시 제거 가능)
    private final MemberQueryService memberQueryService;   // 내 정보 조회
    private final MemberUpdateService memberUpdateService; // 프로필/비번/탈퇴
    private final AddressService addressService;           // ✅ 주소 전담

    /* =========================
     * 마이페이지 인덱스
     * ========================= */
    @GetMapping
    public String index(Model model) {
        MemberDetailDTO me = memberQueryService.getMyInfo();
        if (me == null) return "redirect:/login";

        model.addAttribute("memberDetail", me);
        model.addAttribute("couponCount", 0);      // TODO: 실제 값으로 교체
        model.addAttribute("recentOrders", null);  // TODO: 실제 값으로 교체
        return "mypage/index";
    }

    /* =========================
     * 프로필 조회
     * ========================= */
    @GetMapping("/profile")
    public String profile(Model model) {
        MemberDetailDTO detail = memberQueryService.getMyInfo();
        model.addAttribute("member", detail);
        return "mypage/profile";
    }

    /* =========================
     * 프로필 수정 폼 (연락처/마케팅/비번 + 기본배송지)
     * ========================= */
    @GetMapping("/profile/edit")
    public String profileEditForm(Model model) {
        var me = memberQueryService.getMyInfo(); // 로그인 사용자 정보

        if (!model.containsAttribute("form")) {
            MemberUpdateDTO form = new MemberUpdateDTO();
            form.setPhone(me.getPhone());
            form.setMarketingYn(Boolean.TRUE.equals(me.getMarketingYn()));
            model.addAttribute("form", form);
        }

        // 기본(대표) 배송지 1건을 조회해 바인딩 (없으면 신규 생성 폼 제공)
        Optional<Address> defaultAddrOpt = memberQueryService.findDefaultAddress(me.getMno());
        if (defaultAddrOpt.isPresent()) {
            Address a = defaultAddrOpt.get();
            AddressUpdateDTO addrUpdate = new AddressUpdateDTO();
            addrUpdate.setId(a.getAddressId());
            addrUpdate.setAlias(a.getAlias());
            addrUpdate.setZipCode(a.getZipCode());
            addrUpdate.setAddr1(a.getAddr1());
            addrUpdate.setAddr2(a.getAddr2());
            addrUpdate.setIsDefault(a.isDefault());
            model.addAttribute("addrUpdate", addrUpdate); // 수정용 DTO
            model.addAttribute("addrCreate", null);
        } else {
            model.addAttribute("addrUpdate", null);
            model.addAttribute("addrCreate", new AddressCreateDTO()); // 신규 생성용 DTO
        }

        return "mypage/profile-edit";
    }

    /* =========================
     * 프로필 수정 저장 (연락처/마케팅/비번 + 기본배송지 수정 or 생성)
     * ========================= */
    @PostMapping("/profile/edit")
    public String profileEditSubmit(
            @AuthenticationPrincipal UserDetails user,
            @Valid @ModelAttribute("form") MemberUpdateDTO form,
            BindingResult binding,
            @ModelAttribute(name = "addrUpdate", binding = false) AddressUpdateDTO addrUpdateDto,
            @ModelAttribute(name = "addrCreate", binding = false) AddressCreateDTO addrCreateDto,
            Model model,
            RedirectAttributes ra
    ) {
        var me = memberQueryService.getMyInfo();
        Long memberId = me.getMno();

        // 폼 에러 있으면 주소 블록 채워서 다시 보여줌
        if (binding.hasErrors()) {
            repopulateAddressBlocksForEdit(model, me.getMno(), addrUpdateDto, addrCreateDto);
            return "mypage/profile-edit";
        }

        try {
            // 1) 회원 프로필/비번/마케팅 먼저 저장
            memberUpdateService.updateProfile(memberId, form);

            // 2) 주소 수정/생성 분기 (있을 때만)
            if (addrUpdateDto != null && addrUpdateDto.getId() != null) {
                addressService.update(memberId, addrUpdateDto);     // ✅ 주소 수정
            } else if (isCreatePayload(addrCreateDto)) {
                addressService.create(memberId, addrCreateDto);      // ✅ 주소 생성
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            binding.reject("profileEdit.failed", ex.getMessage());
            repopulateAddressBlocksForEdit(model, me.getMno(), addrUpdateDto, addrCreateDto);
            return "mypage/profile-edit";
        }

        ra.addFlashAttribute("message", "회원정보가 저장되었습니다.");
        return "redirect:/mypage/profile/success";
    }

    /* =========================
     * 탈퇴 화면/처리
     * ========================= */
    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "mypage/withdraw";
    }

    @PostMapping("/withdraw")
    public String withdrawSubmit(
            Authentication auth,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("password") String password,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "memo",   required = false) String memo,
            @RequestParam(value = "agree", defaultValue = "false") boolean agree,
            @RequestParam(value = "confirmText", required = false) String confirmText,
            RedirectAttributes ra
    ) {
        var me = memberQueryService.getMyInfo();
        Long memberId = me.getMno();

        if (!agree || !"회원탈퇴".equals(confirmText)) {
            ra.addFlashAttribute("withdrawError", "탈퇴 확인 항목을 다시 확인해주세요.");
            return "redirect:/mypage/withdraw";
        }

        var dto = new MemberDeleteDTO();
        dto.setCurrentPassword(password);
        dto.setReason(reason);
        dto.setMemo(memo);

        try {
            memberUpdateService.withdraw(memberId, dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("withdrawError", ex.getMessage());
            return "redirect:/mypage/withdraw";
        }

        // 탈퇴 직후 즉시 로그아웃
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                .logout(request, response, auth);

        return "redirect:/mypage/withdraw/success";
    }

    /* =========================
     * 완료 페이지
     * ========================= */
    @GetMapping("/profile/success")
    public String profileEditSuccess() {
        return "mypage/profile-edit-success";
    }

    @GetMapping("/withdraw/success")
    public String withdrawSuccess() {
        return "mypage/withdraw-success";
    }

    /* =========================
     * 주소 REST API (AJAX)
     * ========================= */

    /** 주소 추가 */
    @PostMapping("/address")
    @ResponseBody
    public ResponseEntity<AddressResponseDTO> addAddress(@Valid @RequestBody AddressCreateDTO dto) {
        var me = memberQueryService.getMyInfo();
        var saved = addressService.toDto(addressService.create(me.getMno(), dto));     // ✅ AddressService 사용
        return ResponseEntity.ok(saved);
    }

    /** 주소 수정 */
    @PutMapping(value = "/address/{id}", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<AddressResponseDTO> updateAddress(@PathVariable Long id,
                                                            @RequestBody AddressUpdateDTO dto) {
        var me = memberQueryService.getMyInfo();
        dto.setId(id);
        var saved = addressService.toDto(addressService.update(me.getMno(), dto));
        return ResponseEntity.ok(saved);
    }

    /** 주소 삭제 */
    @DeleteMapping("/address/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        var me = memberQueryService.getMyInfo();
        addressService.delete(me.getMno(), id);                                        // ✅ AddressService 사용
        return ResponseEntity.noContent().build();
    }

    /* =========================
     * 헬퍼
     * ========================= */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private boolean isCreatePayload(AddressCreateDTO dto) {
        if (dto == null) return false;
        return (notBlank(dto.getZipCode())
                || notBlank(dto.getAddr1())
                || notBlank(dto.getAddr2())
                || notBlank(dto.getAlias())
                || (dto.getIsDefault() != null && dto.getIsDefault()));
    }

    /** 에러 시에도 주소 블록이 유지되도록 다시 모델 세팅 */
    private void repopulateAddressBlocksForEdit(Model model, Long memberId,
                                                AddressUpdateDTO addrUpdateDto,
                                                AddressCreateDTO addrCreateDto) {
        Optional<Address> defaultAddrOpt = memberQueryService.findDefaultAddress(memberId);

        if (defaultAddrOpt.isPresent()) {
            if (addrUpdateDto == null || addrUpdateDto.getId() == null) {
                Address a = defaultAddrOpt.get();
                AddressUpdateDTO filled = new AddressUpdateDTO();
                filled.setId(a.getAddressId());
                filled.setAlias(a.getAlias());
                filled.setZipCode(a.getZipCode());
                filled.setAddr1(a.getAddr1());
                filled.setAddr2(a.getAddr2());
                filled.setIsDefault(a.isDefault());
                model.addAttribute("addrUpdate", filled);
            } else {
                model.addAttribute("addrUpdate", addrUpdateDto);
            }
            model.addAttribute("addrCreate", null);
        } else {
            model.addAttribute("addrUpdate", null);
            model.addAttribute("addrCreate", (addrCreateDto != null) ? addrCreateDto : new AddressCreateDTO());
        }
    }
}