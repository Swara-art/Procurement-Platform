
CREATE TABLE tender_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tender_id UUID NOT NULL,

    requirement_code VARCHAR(100) NOT NULL,

    description TEXT NOT NULL,

    operator VARCHAR(30) NOT NULL,

    expected_value TEXT,

    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_requirement_tender
        FOREIGN KEY (tender_id)
        REFERENCES tenders(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_tender_requirement
        UNIQUE (tender_id, requirement_code),

    CONSTRAINT chk_requirement_operator
        CHECK (
            operator IN (
                'EQUALS',
                'GREATER_THAN',
                'GREATER_THAN_OR_EQUAL',
                'LESS_THAN',
                'LESS_THAN_OR_EQUAL',
                'EXISTS',
                'IS_VALID'
            )
        )
);

CREATE INDEX idx_requirements_tender
    ON tender_requirements(tender_id);
