package com.autosetai.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 공통 Entity
 *
 * Entity의 생성, 변경, 삭제 시간에 대한 데이터를 담고 있습니다.
 *
 * createAt - Entity를 생성힌 시간
 * updatedAt - Entity를 수정힌 시간
 * deletedAt - Entity를 삭제한 시간
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Soft Delete 처리를 위한 메서드 캡슐화
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
