
CREATE TABLE tenders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    authority_id UUID NOT NULL,

    tender_number VARCHAR(100) NOT NULL UNIQUE,

    title VARCHAR(255) NOT NULL,

    description TEXT,

    estimated_value NUMERIC(18, 2),

    submission_deadline TIMESTAMPTZ NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tender_authority
        FOREIGN KEY (authority_id)
        REFERENCES organizations(id),

    CONSTRAINT chk_tender_status
        CHECK (
            status IN (
                'DRAFT',
                'PUBLISHED',
                'CLOSED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_tender_value
        CHECK (
            estimated_value IS NULL
            OR estimated_value >= 0
        )
);

CREATE INDEX idx_tenders_authority
    ON tenders(authority_id);
