CREATE TABLE product_categories (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    name_ci VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_product_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_product_categories_tenant_code UNIQUE (tenant_id, code)
);

CREATE UNIQUE INDEX uk_product_categories_tenant_name_ci
    ON product_categories (tenant_id, name_ci);

CREATE INDEX idx_product_categories_tenant_id
    ON product_categories (tenant_id);

CREATE INDEX idx_product_categories_tenant_active
    ON product_categories (tenant_id, active);

CREATE INDEX idx_product_categories_tenant_name_ci
    ON product_categories (tenant_id, name_ci);
