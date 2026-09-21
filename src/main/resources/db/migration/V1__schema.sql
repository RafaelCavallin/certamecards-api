CREATE EXTENSION IF NOT EXISTS citext;

CREATE SEQUENCE sync_seq AS bigint;

CREATE FUNCTION common_stamp_sync_columns() RETURNS trigger AS $$
BEGIN
    NEW.change_seq := nextval('sync_seq');
    NEW.write_xid := pg_current_xact_id();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    email citext NOT NULL,
    email_verified_at timestamptz,
    password_hash text,
    display_name varchar(60) NOT NULL,
    role varchar(16) NOT NULL DEFAULT 'candidate',
    terms_accepted_at timestamptz,
    terms_version varchar(16),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('candidate', 'admin'))
);

CREATE TABLE oauth_identities (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id),
    provider varchar(16) NOT NULL,
    subject varchar(255) NOT NULL,
    email citext NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT oauth_identities_provider_subject_unique UNIQUE (provider, subject),
    CONSTRAINT oauth_identities_provider_check CHECK (provider = 'google')
);
CREATE INDEX oauth_identities_user_id_idx ON oauth_identities (user_id);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id),
    family_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    replaced_by uuid REFERENCES refresh_tokens (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz,
    user_agent varchar(255),
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);

CREATE TABLE one_time_tokens (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id),
    purpose varchar(24) NOT NULL,
    token_hash varchar(64) NOT NULL,
    payload jsonb,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    CONSTRAINT one_time_tokens_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT one_time_tokens_purpose_check
        CHECK (purpose IN ('confirm_email', 'reset_password', 'link_google', 'reauth'))
);
CREATE INDEX one_time_tokens_user_id_idx ON one_time_tokens (user_id);

CREATE TABLE login_attempts (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    email citext NOT NULL,
    ip inet NOT NULL,
    success boolean NOT NULL,
    attempted_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX login_attempts_email_attempted_at_idx ON login_attempts (email, attempted_at);

CREATE TABLE user_settings (
    user_id uuid PRIMARY KEY REFERENCES users (id),
    new_per_day int NOT NULL DEFAULT 20,
    reviews_per_day int NOT NULL DEFAULT 9999,
    focus_minutes int NOT NULL DEFAULT 25,
    exam_date date,
    time_zone varchar(64) NOT NULL,
    theme varchar(8) NOT NULL DEFAULT 'noite',
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT user_settings_new_per_day_check CHECK (new_per_day BETWEEN 0 AND 500),
    CONSTRAINT user_settings_reviews_per_day_check CHECK (reviews_per_day BETWEEN 0 AND 9999),
    CONSTRAINT user_settings_focus_minutes_check CHECK (focus_minutes BETWEEN 15 AND 60),
    CONSTRAINT user_settings_theme_check CHECK (theme IN ('noite', 'dia', 'auto'))
);
CREATE INDEX user_settings_change_seq_idx ON user_settings (change_seq);
CREATE TRIGGER user_settings_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE subjects (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    name varchar(60) NOT NULL,
    normalized_name varchar(60) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT subjects_normalized_name_unique UNIQUE (normalized_name)
);
CREATE INDEX subjects_change_seq_idx ON subjects (change_seq);
CREATE TRIGGER subjects_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON subjects
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE decks (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users (id),
    subject_id uuid NOT NULL REFERENCES subjects (id),
    name varchar(120) NOT NULL,
    description varchar(500),
    origin varchar(24) NOT NULL DEFAULT 'own',
    origin_ref uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    version int NOT NULL DEFAULT 1,
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT decks_origin_check
        CHECK (origin IN ('own', 'official_subscription', 'official_copy', 'community_copy'))
);
CREATE INDEX decks_owner_id_change_seq_idx ON decks (owner_id, change_seq);
CREATE INDEX decks_subject_id_idx ON decks (subject_id);
CREATE TRIGGER decks_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON decks
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE cards (
    id uuid PRIMARY KEY,
    deck_id uuid NOT NULL REFERENCES decks (id),
    type varchar(16) NOT NULL DEFAULT 'basic',
    front varchar(1000) NOT NULL,
    back varchar(2000) NOT NULL,
    source varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    version int NOT NULL DEFAULT 1,
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT cards_type_check CHECK (type IN ('basic', 'cloze', 'true_false'))
);
CREATE INDEX cards_deck_id_change_seq_idx ON cards (deck_id, change_seq);
CREATE TRIGGER cards_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON cards
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE card_states (
    user_id uuid NOT NULL REFERENCES users (id),
    card_id uuid NOT NULL REFERENCES cards (id),
    state smallint NOT NULL DEFAULT 0,
    stability double precision NOT NULL DEFAULT 0,
    difficulty double precision NOT NULL DEFAULT 0,
    due timestamptz NOT NULL,
    last_review timestamptz,
    reps int NOT NULL DEFAULT 0,
    lapses int NOT NULL DEFAULT 0,
    learning_steps int NOT NULL DEFAULT 0,
    scheduled_days int NOT NULL DEFAULT 0,
    review_count int NOT NULL DEFAULT 0,
    suspended boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL DEFAULT now(),
    change_seq bigint,
    write_xid xid8,
    PRIMARY KEY (user_id, card_id),
    CONSTRAINT card_states_state_check CHECK (state BETWEEN 0 AND 3)
);
CREATE INDEX card_states_user_id_change_seq_idx ON card_states (user_id, change_seq);
CREATE INDEX card_states_user_id_due_idx ON card_states (user_id, due);
CREATE TRIGGER card_states_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON card_states
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE review_logs (
    id uuid PRIMARY KEY,
    card_id uuid NOT NULL REFERENCES cards (id),
    kind varchar(8) NOT NULL,
    rating smallint,
    reviewed_at timestamptz NOT NULL,
    duration_ms int NOT NULL,
    state_before jsonb,
    state_after jsonb NOT NULL,
    offline boolean NOT NULL DEFAULT false,
    device_id uuid NOT NULL,
    session_id uuid,
    received_at timestamptz NOT NULL DEFAULT now(),
    change_seq bigint,
    write_xid xid8,
    CONSTRAINT review_logs_kind_check CHECK (kind IN ('review', 'reset')),
    CONSTRAINT review_logs_rating_check CHECK (rating BETWEEN 1 AND 4),
    CONSTRAINT review_logs_duration_ms_check CHECK (duration_ms BETWEEN 0 AND 600000)
);
CREATE INDEX review_logs_card_id_reviewed_at_idx ON review_logs (card_id, reviewed_at);
CREATE INDEX review_logs_change_seq_idx ON review_logs (change_seq);
CREATE TRIGGER review_logs_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON review_logs
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE review_voids (
    review_id uuid PRIMARY KEY REFERENCES review_logs (id),
    voided_at timestamptz NOT NULL,
    change_seq bigint,
    write_xid xid8
);
CREATE INDEX review_voids_change_seq_idx ON review_voids (change_seq);
CREATE TRIGGER review_voids_stamp_sync_columns
    BEFORE INSERT OR UPDATE ON review_voids
    FOR EACH ROW EXECUTE FUNCTION common_stamp_sync_columns();

CREATE TABLE product_events (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES users (id),
    name varchar(48) NOT NULL,
    props jsonb NOT NULL DEFAULT '{}',
    occurred_at timestamptz NOT NULL,
    CONSTRAINT product_events_name_check CHECK (name IN (
        'signup_completed', 'deck_created', 'card_created', 'session_started',
        'session_ended', 'review_undone', 'pwa_installed', 'sync_flushed', 'client_error'
    ))
);
CREATE INDEX product_events_user_id_idx ON product_events (user_id);
