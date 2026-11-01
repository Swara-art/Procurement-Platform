
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) NOT NULL,

    organization_type VARCHAR(50) NOT NULL,

    registration_number VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_organization_name
        UNIQUE (name),

    CONSTRAINT uq_organization_registration
        UNIQUE (registration_number),

    CONSTRAINT chk_organization_type
        CHECK (
            organization_type IN (
                'BIDDER',
                'PROCUREMENT_AUTHORITY'
            )
        )
);

