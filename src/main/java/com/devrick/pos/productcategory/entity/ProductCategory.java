package com.devrick.pos.productcategory.entity;

import com.devrick.pos.common.entity.BaseEntity;
import com.devrick.pos.tenant.entity.Tenant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Locale;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@Entity
@Table(
        name = "product_categories",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_product_categories_tenant_code", columnNames = {"tenant_id", "code"})
        },
        indexes = {
            @Index(name = "idx_product_categories_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_product_categories_tenant_active", columnList = "tenant_id, active")
        })
public class ProductCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Column(name = "name_ci", nullable = false, length = 120)
    private String nameCi;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    private void normalizeFields() {
        if (code != null) {
            code = code.trim().toUpperCase(Locale.ROOT);
        }
        if (name != null) {
            name = name.trim();
            nameCi = name.toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(description)) {
            description = description.trim();
        } else {
            description = null;
        }
    }
}
