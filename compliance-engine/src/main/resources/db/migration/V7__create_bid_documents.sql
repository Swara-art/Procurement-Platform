CREATE TABLE bid_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    bid_id UUID NOT NULL,

    original_filename VARCHAR(255) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    file_size BIGINT NOT NULL,

    storage_key VARCHAR(500) NOT NULL UNIQUE,

    sha256_hash VARCHAR(64) NOT NULL,

    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_document_bid
        FOREIGN KEY (bid_id)
        REFERENCES bids(id),

    CONSTRAINT chk_document_size
        CHECK (file_size > 0)
);

CREATE INDEX idx_bid_documents_bid
    ON bid_documents(bid_id);
