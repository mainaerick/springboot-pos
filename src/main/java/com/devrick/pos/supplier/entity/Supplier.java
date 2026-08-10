package com.devrick.pos.supplier.entity;

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

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@Entity
@Table(
        name = "suppliers",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_suppliers_tenant_code", columnNames = {"tenant_id", "code"})
        },
        indexes = {
            @Index(name = "idx_suppliers_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_suppliers_tenant_active", columnList = "tenant_id, active"),
            @Index(name = "idx_suppliers_tenant_name_ci", columnList = "tenant_id, name_ci")
        })
public class Supplier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Column(name = "name_ci", nullable = false, length = 150)
    private String nameCi;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "alternate_phone", length = 30)
    private String alternatePhone;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

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

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    private void syncDerivedFields() {
        if (name != null) {
            this.nameCi = name.trim().toLowerCase(Locale.ROOT);
        }
        if (code != null) {
            this.code = code.trim().toUpperCase(Locale.ROOT);
        }
    }
}
