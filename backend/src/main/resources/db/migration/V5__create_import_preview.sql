-- The versioned preview a person decides on (issue #12).
--
-- A preview is a proposal, never portfolio state: FR-PREVIEW-004 forbids it
-- from touching the effective snapshot, so nothing here references a snapshot
-- or a confirmed position. The confirmed model arrives with issue #13.
--
-- Money is stored as minor units in BIGINT with an explicit currency (ADR 0005).
-- Nothing financial is buried in JSONB, so the CHECK constraints below can
-- actually defend the invariants.

CREATE TABLE import_preview (
    id                    UUID         PRIMARY KEY,
    import_batch_id       UUID         NOT NULL REFERENCES import_batch (id),
    preview_version       INTEGER      NOT NULL,
    can_commit            BOOLEAN      NOT NULL,
    institution           VARCHAR(120) NOT NULL,
    document_family       VARCHAR(120) NOT NULL,
    layout_version        VARCHAR(60)  NOT NULL,
    parser_id             VARCHAR(120) NOT NULL,
    parser_version        VARCHAR(60)  NOT NULL,
    position_date         DATE,
    generated_at          TIMESTAMPTZ,
    market_reference_date DATE,
    declared_total_minor  BIGINT,
    declared_total_ccy    CHAR(3),
    tolerance_id          VARCHAR(120) NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT import_preview_version_positive CHECK (preview_version > 0),
    -- FR-PREVIEW-002: one row per version, and a version is never rewritten.
    CONSTRAINT import_preview_version_unique UNIQUE (import_batch_id, preview_version),
    CONSTRAINT import_preview_declared_total_is_complete CHECK (
        (declared_total_minor IS NULL AND declared_total_ccy IS NULL)
        OR (declared_total_minor IS NOT NULL AND declared_total_ccy IS NOT NULL)
    )
);

COMMENT ON TABLE import_preview IS
    'A versioned proposal awaiting an explicit human decision. Never portfolio state (FR-PREVIEW-004).';

COMMENT ON COLUMN import_preview.declared_total_minor IS
    'The total the source declared, preserved as published. Never recomputed from the sections (INV-002).';

CREATE TABLE import_preview_position (
    id                UUID          PRIMARY KEY,
    preview_id        UUID          NOT NULL REFERENCES import_preview (id) ON DELETE CASCADE,
    ordinal           INTEGER       NOT NULL,
    category          VARCHAR(40)   NOT NULL,
    currency          CHAR(3)       NOT NULL,
    description_raw   TEXT          NOT NULL,
    asset_code_raw    VARCHAR(120),
    quantity          NUMERIC(38, 12),
    unit_price        NUMERIC(38, 8),
    gross_minor       BIGINT,
    gross_quality     VARCHAR(16)   NOT NULL,
    page_number       INTEGER       NOT NULL,
    evidence_token    TEXT          NOT NULL,
    resolution        VARCHAR(24)   NOT NULL,
    resolution_reason VARCHAR(40),
    confidence        VARCHAR(16)   NOT NULL,
    fingerprint       VARCHAR(400)  NOT NULL,
    CONSTRAINT import_preview_position_ordinal_unique UNIQUE (preview_id, ordinal),
    CONSTRAINT import_preview_position_quality_is_known CHECK (
        gross_quality IN ('EXACT', 'ESTIMATED', 'UNKNOWN')
    ),
    -- INV-003 in the schema: an unknown amount is absent, never zero, and a
    -- known amount always carries a value.
    CONSTRAINT import_preview_position_unknown_has_no_amount CHECK (
        (gross_quality = 'UNKNOWN' AND gross_minor IS NULL)
        OR (gross_quality <> 'UNKNOWN' AND gross_minor IS NOT NULL)
    ),
    CONSTRAINT import_preview_position_resolution_is_known CHECK (
        resolution IN ('RESOLVED', 'REVIEW_REQUIRED')
    ),
    CONSTRAINT import_preview_position_review_has_reason CHECK (
        (resolution = 'RESOLVED' AND resolution_reason IS NULL)
        OR (resolution = 'REVIEW_REQUIRED' AND resolution_reason IS NOT NULL)
    )
);

COMMENT ON COLUMN import_preview_position.gross_quality IS
    'EXACT, ESTIMATED or UNKNOWN. UNKNOWN keeps the amount null: absent is not zero (INV-003).';

-- Warnings and blockers, from reconciliation, normalization and asset review.
-- One table because the preview presents them as one list to the person
-- deciding, and splitting by origin would make "why can I not commit?" a join.
CREATE TABLE import_preview_finding (
    id               UUID         PRIMARY KEY,
    preview_id       UUID         NOT NULL REFERENCES import_preview (id) ON DELETE CASCADE,
    ordinal          INTEGER      NOT NULL,
    origin           VARCHAR(24)  NOT NULL,
    severity         VARCHAR(16)  NOT NULL,
    code             VARCHAR(80)  NOT NULL,
    subject          TEXT         NOT NULL,
    page_number      INTEGER,
    currency         CHAR(3),
    declared_minor   BIGINT,
    calculated_minor BIGINT,
    difference_minor BIGINT,
    tolerance_minor  BIGINT,
    CONSTRAINT import_preview_finding_ordinal_unique UNIQUE (preview_id, ordinal),
    CONSTRAINT import_preview_finding_origin_is_known CHECK (
        origin IN ('RECONCILIATION', 'NORMALIZATION', 'ASSET_REVIEW')
    ),
    CONSTRAINT import_preview_finding_severity_is_known CHECK (
        severity IN ('INFO', 'WARNING', 'BLOCKER', 'NOT_EVIDENCED')
    )
);

COMMENT ON TABLE import_preview_finding IS
    'Everything the person must see before deciding. A BLOCKER row forbids commit (FR-RECON-005).';

CREATE INDEX import_preview_batch_idx ON import_preview (import_batch_id, preview_version DESC);

CREATE INDEX import_preview_finding_severity_idx ON import_preview_finding (preview_id, severity);
