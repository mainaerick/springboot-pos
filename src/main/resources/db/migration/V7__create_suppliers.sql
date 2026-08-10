CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name_ci VARCHAR(150) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(150),
    email VARCHAR(254),
    phone VARCHAR(30),
    alternate_phone VARCHAR(30),
    tax_number VARCHAR(50),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state_or_county VARCHAR(100),
    postal_code VARCHAR(30),
    country_code VARCHAR(2),
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_suppliers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_suppliers_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_suppliers_tenant_id
    ON suppliers (tenant_id);

CREATE INDEX idx_suppliers_tenant_active
    ON suppliers (tenant_id, active);

CREATE INDEX idx_suppliers_tenant_name_ci
    ON suppliers (tenant_id, name_ci);
