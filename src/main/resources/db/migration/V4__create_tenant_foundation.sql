CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_tenants_code UNIQUE (code)
);

CREATE INDEX idx_tenants_status ON tenants(status);

INSERT INTO tenants (
        id,
        name,
        code,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version,
        deleted)
SELECT
        '55555555-5555-5555-5555-555555555555',
        'Default Business',
        'DEFAULT',
        'ACTIVE',
        TIMESTAMP WITH TIME ZONE '2026-08-03 00:00:00+00',
        TIMESTAMP WITH TIME ZONE '2026-08-03 00:00:00+00',
        NULL,
        NULL,
        0,
        FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM tenants
    WHERE code = 'DEFAULT'
);

ALTER TABLE users ADD COLUMN tenant_id UUID;

UPDATE users
SET tenant_id = '55555555-5555-5555-5555-555555555555'
WHERE tenant_id IS NULL;

ALTER TABLE users
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
