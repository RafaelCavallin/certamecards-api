CREATE TABLE sync_purge_state (
    id boolean PRIMARY KEY DEFAULT true,
    watermark_change_seq bigint NOT NULL DEFAULT 0,
    CONSTRAINT sync_purge_state_single_row CHECK (id)
);

INSERT INTO sync_purge_state (id, watermark_change_seq) VALUES (true, 0);
