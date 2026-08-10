CREATE TABLE branches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    name_ci VARCHAR(120) NOT NULL,
    code VARCHAR(30) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(30),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state_or_county VARCHAR(100),
    postal_code VARCHAR(30),
    country_code VARCHAR(2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_branches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_branches_tenant_code UNIQUE (tenant_id, code)
);

CREATE UNIQUE INDEX uk_branches_tenant_name_ci
    ON branches (tenant_id, name_ci);

CREATE INDEX idx_branches_tenant_id
    ON branches (tenant_id);

CREATE INDEX idx_branches_tenant_active
    ON branches (tenant_id, active);
