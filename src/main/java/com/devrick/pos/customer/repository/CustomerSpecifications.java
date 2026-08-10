package com.devrick.pos.customer.repository;

import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<Customer> byTenantAndFilters(
            UUID tenantId, String search, CustomerType type, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            if (StringUtils.hasText(search)) {
                String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate codeMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), normalizedSearch);
                Predicate nameMatches = criteriaBuilder.like(root.get("nameCi"), normalizedSearch);
                Predicate emailMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), normalizedSearch);
                Predicate phoneMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), normalizedSearch);
                Predicate taxNumberMatches =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("taxNumber")), normalizedSearch);
                predicates.add(criteriaBuilder.or(codeMatches, nameMatches, emailMatches, phoneMatches, taxNumberMatches));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
