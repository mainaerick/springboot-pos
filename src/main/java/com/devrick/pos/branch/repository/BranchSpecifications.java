package com.devrick.pos.branch.repository;

import com.devrick.pos.branch.entity.Branch;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class BranchSpecifications {

    private BranchSpecifications() {}

    public static Specification<Branch> byTenantAndFilters(UUID tenantId, String search, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            if (StringUtils.hasText(search)) {
                String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate nameMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), normalizedSearch);
                Predicate codeMatches = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), normalizedSearch);
                predicates.add(criteriaBuilder.or(nameMatches, codeMatches));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
