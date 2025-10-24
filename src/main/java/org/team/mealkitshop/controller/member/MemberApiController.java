package org.team.mealkitshop.controller.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.service.member.MemberService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/email/check")
    public Map<String, Object> checkEmail(@RequestParam("email") String email) {
        boolean exists = memberService.existsByEmail(email);
        return Map.of("email", email, "available", !exists);
    }

    @PostMapping
    public ResponseEntity<?> register(@RequestBody @Valid JsonSignupRequest req) {
        if (memberService.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 사용 중인 이메일입니다."));
        }
        Member m = Member.builder()
                .email(req.email())
                .password(req.password())
                .memberName(req.memberName())
                .phone(req.phone().replaceAll("[^0-9]", ""))
                .grade(Grade.BASIC).points(0)
                .role(Role.USER).provider(Provider.Local).status(Status.ACTIVE)
                .marketingYn(Boolean.TRUE.equals(req.marketingYn())) // ✅ 여기도 통일
                .build();

        Member saved = memberService.saveMember(m);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getMno(), "email", saved.getEmail(), "memberName", saved.getMemberName()));
    }

    public record JsonSignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min=8, max=64) String password,
            @NotBlank @Size(min=2, max=20) String memberName,
            @NotBlank @Pattern(regexp="\\d{9,13}") String phone,
            Boolean marketingYn
    ) {}
}
