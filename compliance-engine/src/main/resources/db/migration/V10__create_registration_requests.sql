CREATE TABLE account_registration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('ORGANIZATION','ADMIN')),
    organization_name VARCHAR(255),
    registration_number VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    provisioned_user_id UUID REFERENCES users(id),
    CONSTRAINT ck_registration_request_org_fields CHECK (
      (actor_type = 'ORGANIZATION' AND organization_name IS NOT NULL AND registration_number IS NOT NULL)
      OR (actor_type = 'ADMIN' AND organization_name IS NULL AND registration_number IS NULL)
    )
);
CREATE UNIQUE INDEX uq_pending_registration_email
    ON account_registration_requests (LOWER(email)) WHERE status = 'PENDING';
CREATE INDEX idx_registration_requests_status_time
    ON account_registration_requests (status, submitted_at);
