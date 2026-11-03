CREATE TABLE sectors (
    code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL UNIQUE
);

INSERT INTO sectors(code, display_name) VALUES
 ('IT_SOFTWARE','IT Software'), ('HEALTHCARE_IT','Healthcare IT'), ('CLOUD_INFRASTRUCTURE','Cloud Infrastructure'),
 ('MEDICAL_EQUIPMENT','Medical Equipment'), ('PHARMACEUTICALS','Pharmaceuticals'), ('CIVIL_CONSTRUCTION','Civil Construction'),
 ('ELECTRICAL','Electrical'), ('AGRICULTURE','Agriculture'), ('EDUCATION_SERVICES','Education Services'),
 ('TRANSPORTATION','Transportation'), ('CONSULTING','Consulting');

CREATE TABLE sector_relationships (
    sector_code VARCHAR(50) NOT NULL REFERENCES sectors(code),
    related_sector_code VARCHAR(50) NOT NULL REFERENCES sectors(code),
    PRIMARY KEY (sector_code, related_sector_code),
    CHECK (sector_code <> related_sector_code)
);
INSERT INTO sector_relationships VALUES
 ('HEALTHCARE_IT','IT_SOFTWARE'), ('IT_SOFTWARE','CLOUD_INFRASTRUCTURE'), ('CLOUD_INFRASTRUCTURE','IT_SOFTWARE');

CREATE TABLE bidder_sectors (
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sector_code VARCHAR(50) NOT NULL REFERENCES sectors(code),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (organization_id, sector_code)
);
CREATE UNIQUE INDEX uq_bidder_primary_sector ON bidder_sectors(organization_id) WHERE is_primary;
CREATE INDEX idx_bidder_sectors_sector ON bidder_sectors(sector_code, organization_id);

CREATE TABLE bidder_capabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    capability VARCHAR(120) NOT NULL,
    normalized_capability VARCHAR(120) NOT NULL,
    UNIQUE (organization_id, normalized_capability)
);

ALTER TABLE bidder_profiles
    ADD COLUMN entity_type VARCHAR(40),
    ADD COLUMN contact_number VARCHAR(30),
    ADD COLUMN establishment_date DATE,
    ADD COLUMN experience_years INTEGER,
    ADD COLUMN completed_projects INTEGER,
    ADD COLUMN gst_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN authorized_signatory_name VARCHAR(200),
    ADD COLUMN authorized_signatory_email VARCHAR(320),
    ADD COLUMN onboarding_status VARCHAR(30) NOT NULL DEFAULT 'PROFILE_INCOMPLETE'
      CHECK (onboarding_status IN ('ACCOUNT_CREATED','PROFILE_INCOMPLETE','DOCUMENTS_PENDING','VERIFICATION_PENDING','VERIFIED','REJECTED','SUSPENDED'));

CREATE TABLE bidder_financials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL,
    annual_turnover NUMERIC(18,2) NOT NULL CHECK (annual_turnover >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    UNIQUE (organization_id, financial_year)
);

CREATE TABLE tender_classification_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REVIEW_REQUIRED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    model_version VARCHAR(100),
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tender_id, content_hash)
);
CREATE INDEX idx_classification_jobs_ready ON tender_classification_jobs(status, next_attempt_at, created_at);

CREATE TABLE tender_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES tender_classification_jobs(id) ON DELETE CASCADE,
    tender_id UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    sector_code VARCHAR(50) NOT NULL REFERENCES sectors(code),
    relevance_score NUMERIC(5,4) NOT NULL CHECK (relevance_score >= 0 AND relevance_score <= 1),
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(20) NOT NULL CHECK (source IN ('MODEL','RULES','HUMAN')),
    confirmed_by UUID REFERENCES users(id),
    confirmed_at TIMESTAMPTZ,
    UNIQUE (job_id, sector_code)
);
CREATE INDEX idx_tender_classifications_match ON tender_classifications(confirmed, sector_code, tender_id);

CREATE TABLE document_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    document_id UUID,
    event_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
