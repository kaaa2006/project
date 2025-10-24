package org.team.mealkitshop.domain.member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.board.ReviewBoard;

import java.util.List;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")

public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MariaDB 권장
    private Long mno; // 내부 식별용 PK

    @Column(length = 100, nullable = false, unique = true)
    private String email; // 로그인 ID

    @Column(length = 20, nullable = false, unique = true)
    private String memberName; // 이름 (중복 방지용 unique 설정)

    @Column(length = 60, nullable = false) // BCrypt 해시 길이 = 60
    private String password; // 해시 저장

    @Column(length = 20, nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Grade grade = Grade.BASIC; // 회원 등급

    @Builder.Default
    @Column(nullable = false)
    private Integer points = 0; // 포인트

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "marketing_yn", length = 1, nullable = false)
    private boolean marketingYn; // 마케팅 수신 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;  // constant.Role 사용자, 관리자 구분용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Provider provider = Provider.Local; // 카카오, 일반회원 구분용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE; // 탈퇴여부

    @OneToMany(mappedBy = "writerMember")
    @JsonIgnore
    private List<ReviewBoard> boards;

}
