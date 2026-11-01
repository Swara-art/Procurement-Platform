
CREATE TABLE bids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tender_id UUID NOT NULL,

    bidder_id UUID NOT NULL,

    submitted_at TIMESTAMPTZ,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bid_tender
        FOREIGN KEY (tender_id)
        REFERENCES tenders(id),

    CONSTRAINT fk_bid_bidder
        FOREIGN KEY (bidder_id)
        REFERENCES bidder_profiles(id),

    CONSTRAINT uq_bid_tender_bidder
        UNIQUE (tender_id, bidder_id),

    CONSTRAINT chk_bid_status
        CHECK (
            status IN (
                'DRAFT',
                'SUBMITTED',
                'UNDER_REVIEW',
                'EVALUATED',
                'WITHDRAWN'
            )
        )
);

CREATE INDEX idx_bids_tender
    ON bids(tender_id);

CREATE INDEX idx_bids_bidder
    ON bids(bidder_id);
