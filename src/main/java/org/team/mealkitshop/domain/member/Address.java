package org.team.mealkitshop.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 50)
    private String alias; // ex: 집, 회사, 부모님 댁

    @Column(name = "zipcode", length = 10, nullable = false)
    private String zipCode;

    @Column(length = 100, nullable = false)
    private String addr1; // 도로명 주소

    @Column(length = 100)
    private String addr2; // 상세 주소 (건물명, 동호수 등)

    @Column(nullable = false)
    private boolean isDefault = false;

}