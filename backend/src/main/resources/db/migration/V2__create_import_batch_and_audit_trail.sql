-- Import lifecycle and audit trail (issue #6).
--
-- No portfolio table is created here: this migration carries the state machine
-- and the audit trail only. Financial data arrives with later issues.

CREATE TABLE import_batch (
    id                   UUID         PRIMARY KEY,
    status               VARCHAR(32)  NOT NULL,
    version              BIGINT       NOT NULL,
    preview_version      INTEGER      NOT NULL,
    raw_sha256           CHAR(64)     NOT NULL,
    semantic_fingerprint VARCHAR(128),
    parser_id            VARCHAR(120),
    parser_version       VARCHAR(60),
    correlation_id       UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT import_batch_version_non_negative CHECK (version >= 0),
    CONSTRAINT import_batch_preview_version_non_negative CHECK (preview_version >= 0),
    CONSTRAINT import_batch_raw_sha256_is_hex CHECK (raw_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT import_batch_status_is_known CHECK (
        status IN (
            'RECEIVED', 'FINGERPRINTED', 'PASSWORD_REQUIRED', 'EXTRACTING',
            'LAYOUT_DETECTED', 'PARSING', 'NORMALIZING', 'RECONCILING',
            'PREVIEW_READY', 'BLOCKED', 'COMMITTING', 'COMMITTED',
            'REJECTED', 'FAILED', 'DUPLICATE'
        )
    ),
    -- Parser identity is all-or-nothing.
    CONSTRAINT import_batch_parser_is_complete CHECK (
        (parser_id IS NULL AND parser_version IS NULL)
        OR (parser_id IS NOT NULL AND parser_version IS NOT NULL)
    )
);

COMMENT ON TABLE import_batch IS
    'Import lifecycle. version guards the aggregate; preview_version guards the user decision.';

CREATE INDEX import_batch_status_idx ON import_batch (status);

CREATE INDEX import_batch_raw_sha256_idx ON import_batch (raw_sha256);

-- Append-only. INV-019 forbids editing a recorded event to "correct" history,
-- so no UPDATE or DELETE path exists in the application, and the trail carries
-- no foreign key that could cascade a delete onto it.
CREATE TABLE audit_event (
    id             UUID         PRIMARY KEY,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    actor          VARCHAR(120) NOT NULL,
    action         VARCHAR(120) NOT NULL,
    subject        VARCHAR(120) NOT NULL,
    correlation_id UUID         NOT NULL,
    details        JSONB        NOT NULL DEFAULT '{}'::JSONB,
    CONSTRAINT audit_event_actor_not_blank CHECK (length(btrim(actor)) > 0),
    CONSTRAINT audit_event_subject_not_blank CHECK (length(btrim(subject)) > 0)
);

COMMENT ON TABLE audit_event IS
    'Append-only audit trail. Never updated or deleted by the application (INV-019).';

CREATE INDEX audit_event_subject_idx ON audit_event (subject, occurred_at);

CREATE INDEX audit_event_correlation_idx ON audit_event (correlation_id);
