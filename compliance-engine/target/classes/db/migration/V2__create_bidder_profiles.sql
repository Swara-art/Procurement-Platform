
CREATE TABLE bidder_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    organization_id UUID NOT NULL UNIQUE,

    pan_number VARCHAR(10),
    gstin VARCHAR(15),
    cin VARCHAR(21),

    business_address TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bidder_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uq_bidder_pan UNIQUE (pan_number),
    CONSTRAINT uq_bidder_gstin UNIQUE (gstin),
    CONSTRAINT uq_bidder_cin UNIQUE (cin)
);
