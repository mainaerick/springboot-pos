package com.devrick.pos.supplier.repository;

import com.devrick.pos.supplier.entity.Supplier;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SupplierSpecifications {

    private SupplierSpecifications() {}

    public static Specification<Supplier> byTenantAndFilters(UUID tenantId, String search, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            if (StringUtils.hasText(search)) {
                String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate codeMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), normalizedSearch);
                Predicate nameMatches = criteriaBuilder.like(root.get("nameCi"), normalizedSearch);
                Predicate contactPersonMatches =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("contactPerson")), normalizedSearch);
                Predicate emailMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), normalizedSearch);
                Predicate phoneMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), normalizedSearch);
                Predicate alternatePhoneMatches =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("alternatePhone")), normalizedSearch);
                Predicate taxNumberMatches =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("taxNumber")), normalizedSearch);
                predicates.add(criteriaBuilder.or(
                        codeMatches,
                        nameMatches,
                        contactPersonMatches,
                        emailMatches,
                        phoneMatches,
                        alternatePhoneMatches,
                        taxNumberMatches));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
