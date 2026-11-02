ALTER TABLE bids
    ADD COLUMN quoted_amount NUMERIC(18, 2);

ALTER TABLE bids
    ADD CONSTRAINT chk_bid_quoted_amount
        CHECK (quoted_amount IS NULL OR quoted_amount > 0);
