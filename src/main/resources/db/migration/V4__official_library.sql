ALTER TABLE decks ALTER COLUMN owner_id DROP NOT NULL;

ALTER TABLE decks
    ADD COLUMN official_status varchar(16),
    ADD COLUMN origin_label varchar(120),
    ADD COLUMN content_updated_at timestamptz,
    ADD COLUMN card_count int NOT NULL DEFAULT 0,
    ADD COLUMN subscriber_count int NOT NULL DEFAULT 0,
    ADD COLUMN search_text varchar(640);

ALTER TABLE decks
    ADD CONSTRAINT decks_official_shape_check CHECK (
        (owner_id IS NOT NULL AND official_status IS NULL)
        OR (owner_id IS NULL AND origin = 'official_subscription' AND official_status IN ('draft', 'published', 'discontinued'))
    );

ALTER TABLE decks
    ADD CONSTRAINT decks_official_counts_check CHECK (
        official_status IS NOT NULL OR (card_count = 0 AND subscriber_count = 0)
    );

ALTER TABLE decks
    ADD CONSTRAINT decks_official_status_check CHECK (official_status IN ('draft', 'published', 'discontinued'));

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX decks_library_search_idx ON decks USING gin (search_text gin_trgm_ops)
    WHERE official_status = 'published';

ALTER TABLE review_logs ADD COLUMN user_id uuid REFERENCES users (id);

UPDATE review_logs rl
SET user_id = d.owner_id
FROM cards c
JOIN decks d ON d.id = c.deck_id
WHERE c.id = rl.card_id AND rl.user_id IS NULL;

ALTER TABLE review_logs ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX review_logs_user_card_idx ON review_logs (user_id, card_id, reviewed_at);
CREATE INDEX review_logs_user_change_seq_idx ON review_logs (user_id, change_seq);

ALTER TABLE review_logs DROP CONSTRAINT review_logs_kind_check;
ALTER TABLE review_logs
    ADD CONSTRAINT review_logs_kind_check CHECK (kind IN ('review', 'reset', 'content_update', 'duplicate'));

ALTER TABLE card_states
    ADD COLUMN content_update_note varchar(200),
    ADD COLUMN content_updated_at timestamptz;

ALTER TABLE product_events DROP CONSTRAINT product_events_name_check;
ALTER TABLE product_events
    ADD CONSTRAINT product_events_name_check CHECK (name IN (
        'signup_completed', 'deck_created', 'card_created', 'session_started',
        'session_ended', 'review_undone', 'pwa_installed', 'sync_flushed', 'client_error',
        'library_opened', 'library_searched', 'deck_preview_opened', 'deck_subscribed',
        'deck_unsubscribed', 'deck_duplicated', 'card_error_reported'
    ));

CREATE TABLE deck_subscriptions (
    user_id uuid NOT NULL REFERENCES users (id),
    deck_id uuid NOT NULL REFERENCES decks (id),
    subscribed_at timestamptz NOT NULL DEFAULT now(),
    cancelled_at timestamptz,
    progress_purged_at timestamptz,
    change_seq bigint,
    write_xid xid8,
    PRIMARY KEY (user_id, deck_id)
);
CREATE INDEX deck_subscriptions_user_change_seq_idx ON deck_subscriptions (user_id, change_seq);
CREATE INDEX deck_subscriptions_active_idx ON deck_subscriptions (deck_id, user_id) WHERE cancelled_at IS NULL;
CREATE INDEX deck_subscriptions_purge_idx ON deck_subscriptions (cancelled_at)
    WHERE cancelled_at IS NOT NULL AND progress_purged_at IS NULL;
CREATE TRIGGER deck_subscriptions_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON deck_subscriptions
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE card_error_reports (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    card_id uuid NOT NULL REFERENCES cards (id),
    user_id uuid NOT NULL REFERENCES users (id),
    reason varchar(24) NOT NULL,
    note varchar(500),
    status varchar(16) NOT NULL DEFAULT 'open',
    created_at timestamptz NOT NULL DEFAULT now(),
    closed_at timestamptz,
    closed_by uuid REFERENCES users (id),
    CONSTRAINT card_error_reports_card_user_unique UNIQUE (card_id, user_id),
    CONSTRAINT card_error_reports_reason_check
        CHECK (reason IN ('outdated_content', 'wrong_answer', 'typo', 'other')),
    CONSTRAINT card_error_reports_status_check CHECK (status IN ('open', 'resolved', 'rejected'))
);
CREATE INDEX card_error_reports_open_idx ON card_error_reports (created_at) WHERE status = 'open';
CREATE INDEX card_error_reports_card_idx ON card_error_reports (card_id);

CREATE TABLE admin_audit_logs (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    actor_id uuid NOT NULL REFERENCES users (id),
    action varchar(40) NOT NULL,
    target_type varchar(24) NOT NULL,
    target_id uuid,
    target_label varchar(160),
    changes jsonb NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX admin_audit_logs_created_at_idx ON admin_audit_logs (created_at DESC, id DESC);
CREATE INDEX admin_audit_logs_actor_idx ON admin_audit_logs (actor_id, created_at DESC);
CREATE INDEX admin_audit_logs_action_idx ON admin_audit_logs (action, created_at DESC);

CREATE FUNCTION admin_audit_logs_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'admin_audit_logs is append-only';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER admin_audit_logs_append_only_trigger
    BEFORE UPDATE OR DELETE ON admin_audit_logs
    FOR EACH ROW EXECUTE FUNCTION admin_audit_logs_append_only();

CREATE TABLE official_content_update_jobs (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    card_id uuid NOT NULL REFERENCES cards (id),
    deck_id uuid NOT NULL REFERENCES decks (id),
    note varchar(200) NOT NULL,
    updated_at timestamptz NOT NULL,
    cursor_user_id uuid,
    applied_count int NOT NULL DEFAULT 0,
    finished_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX official_content_update_jobs_pending_idx
    ON official_content_update_jobs (created_at) WHERE finished_at IS NULL;
