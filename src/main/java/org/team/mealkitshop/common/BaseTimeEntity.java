package org.team.mealkitshop.common;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter //  데이터베이스 날짜를 가져오기만 한다. (보안상)
@MappedSuperclass   // 공통적 최상위 클래스
@EntityListeners(AuditingEntityListener.class)  // 감시용 클래스 명시
public class BaseTimeEntity {

    @CreatedDate
    @Column(name = "reg_time", updatable = false, nullable = false)
    private LocalDateTime regTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
