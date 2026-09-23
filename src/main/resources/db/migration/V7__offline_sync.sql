CREATE TABLE sync_operation_receipts (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    operation_id uuid NOT NULL,
    request_hash varchar(64) NOT NULL,
    kind varchar(24) NOT NULL,
    event_at timestamptz NOT NULL,
    event_counter int NOT NULL DEFAULT 0,
    event_device_id uuid NOT NULL,
    outcome varchar(24) NOT NULL,
    entity_version int,
    change_seq bigint,
    conflict_id uuid,
    error_code varchar(40),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, operation_id)
);
CREATE INDEX sync_operation_receipts_created_at_idx ON sync_operation_receipts (created_at);

CREATE TABLE sync_entity_heads (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entity_type varchar(16) NOT NULL,
    entity_id uuid NOT NULL,
    parent_id uuid,
    version int NOT NULL DEFAULT 0,
    winning_operation_id uuid,
    event_at timestamptz,
    event_counter int,
    event_device_id uuid,
    deleted boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, entity_type, entity_id),
    CONSTRAINT sync_entity_heads_entity_type_check CHECK (entity_type IN ('deck', 'card'))
);
CREATE INDEX sync_entity_heads_parent_idx ON sync_entity_heads (user_id, parent_id);

CREATE TABLE sync_setting_field_clocks (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    field_name varchar(32) NOT NULL,
    operation_id uuid NOT NULL,
    event_at timestamptz NOT NULL,
    event_counter int NOT NULL DEFAULT 0,
    event_device_id uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, field_name)
);

CREATE TABLE sync_conflicts (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entity_type varchar(16) NOT NULL,
    entity_id uuid NOT NULL,
    deck_id uuid,
    losing_operation_id uuid NOT NULL,
    winning_operation_id uuid,
    reason varchar(24) NOT NULL,
    losing_snapshot jsonb,
    winning_snapshot jsonb,
    expires_at timestamptz NOT NULL,
    restored_at timestamptz,
    expired_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT sync_conflicts_losing_operation_unique UNIQUE (losing_operation_id),
    CONSTRAINT sync_conflicts_entity_type_check CHECK (entity_type IN ('deck', 'card')),
    CONSTRAINT sync_conflicts_reason_check CHECK (reason IN ('concurrent_edit', 'delete_wins', 'parent_deleted'))
);
CREATE INDEX sync_conflicts_user_expires_idx ON sync_conflicts (user_id, expires_at);
CREATE INDEX sync_conflicts_purge_idx ON sync_conflicts (expires_at) WHERE expired_at IS NULL;
CREATE TRIGGER sync_conflicts_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON sync_conflicts
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

ALTER TABLE review_logs
    ADD COLUMN event_at timestamptz,
    ADD COLUMN event_counter int,
    ADD COLUMN event_device_id uuid,
    ADD COLUMN operation_id uuid;

UPDATE review_logs
SET event_at = reviewed_at,
    event_counter = 0,
    event_device_id = device_id,
    operation_id = id
WHERE event_at IS NULL;

ALTER TABLE review_logs
    ALTER COLUMN event_at SET NOT NULL,
    ALTER COLUMN event_counter SET NOT NULL,
    ALTER COLUMN event_counter SET DEFAULT 0,
    ALTER COLUMN event_device_id SET NOT NULL,
    ALTER COLUMN operation_id SET NOT NULL;

ALTER TABLE review_logs ADD CONSTRAINT review_logs_operation_id_unique UNIQUE (operation_id);

CREATE INDEX review_logs_user_card_canonical_idx
    ON review_logs (user_id, card_id, event_at, event_counter, event_device_id, operation_id);

ALTER TABLE users ADD COLUMN change_seq bigint, ADD COLUMN write_xid xid8;
CREATE INDEX users_change_seq_idx ON users (change_seq);
CREATE TRIGGER users_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();
