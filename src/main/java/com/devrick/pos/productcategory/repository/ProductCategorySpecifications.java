package com.devrick.pos.productcategory.repository;

import com.devrick.pos.productcategory.entity.ProductCategory;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductCategorySpecifications {

    private ProductCategorySpecifications() {}

    public static Specification<ProductCategory> byTenantAndFilters(UUID tenantId, String search, Boolean active) {
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
                Predicate descriptionMatches =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), normalizedSearch);
                predicates.add(criteriaBuilder.or(codeMatches, nameMatches, descriptionMatches));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
