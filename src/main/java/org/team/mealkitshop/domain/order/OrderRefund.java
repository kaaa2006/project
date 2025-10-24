package org.team.mealkitshop.domain.order;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.common.RefundReason;
import org.team.mealkitshop.common.RefundStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundReason reasonCode;

    @Column(length = 500)
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    // ✅ 관리자 처리 정보
    @Column(length = 50)
    private String processedBy;        // 처리한 관리자 닉네임

    private LocalDateTime processedAt; // 처리 시각
}
