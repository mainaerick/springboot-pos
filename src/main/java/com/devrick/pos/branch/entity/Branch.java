package com.devrick.pos.branch.entity;

import com.devrick.pos.common.entity.BaseEntity;
import com.devrick.pos.tenant.entity.Tenant;
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

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_branches_tenant_code", columnNames = {"tenant_id", "code"})
        },
        indexes = {
            @Index(name = "idx_branches_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_branches_tenant_active", columnList = "tenant_id, active")
        })
public class Branch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Column(name = "name_ci", nullable = false, length = 120)
    private String nameCi;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_or_county", length = 100)
    private String stateOrCounty;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    private void syncNameCi() {
        if (name == null) {
            return;
        }

        this.nameCi = name.trim().toLowerCase(Locale.ROOT);
    }
}
