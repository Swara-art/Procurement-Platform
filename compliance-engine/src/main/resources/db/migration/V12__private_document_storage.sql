ALTER TABLE bid_documents
    ADD COLUMN document_type VARCHAR(30) NOT NULL DEFAULT 'COMPLIANCE',
    ADD COLUMN storage_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN bucket_identifier VARCHAR(255),
    ADD COLUMN verification_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN processing_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN superseded_at TIMESTAMPTZ,
    ADD COLUMN uploaded_by UUID REFERENCES users(id);
CREATE INDEX idx_bid_documents_stage ON bid_documents(bid_id, document_type, superseded_at);

CREATE TABLE organization_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(40) NOT NULL,
    financial_year VARCHAR(9),
    object_key VARCHAR(700) NOT NULL UNIQUE,
    storage_provider VARCHAR(20) NOT NULL,
    bucket_identifier VARCHAR(255),
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    sha256_checksum VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    verification_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    processing_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    superseded_at TIMESTAMPTZ
);
CREATE INDEX idx_org_documents_owner ON organization_documents(organization_id, document_type, superseded_at);
CREATE INDEX idx_org_documents_verification ON organization_documents(verification_status, processing_status);

CREATE TABLE document_requirement_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40),
    sector_code VARCHAR(50) REFERENCES sectors(code),
    jurisdiction VARCHAR(100),
    registration_policy VARCHAR(100),
    required BOOLEAN NOT NULL DEFAULT TRUE,
    conditional_expression VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO document_requirement_rules(document_type,required,conditional_expression) VALUES
 ('BUSINESS_REGISTRATION',TRUE,NULL),('PAN',TRUE,NULL),('GST_CERTIFICATE',FALSE,'gstApplicable=true'),
 ('SIGNATORY_AUTHORIZATION',TRUE,NULL),('ADDRESS_PROOF',TRUE,NULL),('FINANCIAL_STATEMENT',TRUE,'at least one completed financial year'),
 ('CAPABILITY_DECLARATION',TRUE,NULL),('UDYAM_MSME',FALSE,'claimed MSME status'),('ENTITY_DEED',FALSE,'entityType in PARTNERSHIP,LLP'),
 ('EXPERIENCE_PROOF',FALSE,'experience claimed'),('ISO_CERTIFICATE',FALSE,'sector or tender requirement'),
 ('SECTOR_LICENSE',FALSE,'sector or policy requirement'),('NON_BLACKLISTING',FALSE,'jurisdictional policy'),('OTHER_STATUTORY',FALSE,'jurisdictional policy');
