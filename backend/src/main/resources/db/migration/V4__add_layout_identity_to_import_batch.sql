-- Persists the layout a parser was selected for (issue #8).
--
-- FR-PARSER-004 requires the parser version used to be persisted, and a parser
-- version is meaningless without the layout it answers to. The three columns
-- travel with parser_id and parser_version as one unit.

ALTER TABLE import_batch
    ADD COLUMN institution     VARCHAR(120),
    ADD COLUMN document_family VARCHAR(120),
    ADD COLUMN layout_version  VARCHAR(60);

COMMENT ON COLUMN import_batch.layout_version IS
    'Layout the parser was selected for. A producer format change means a new version, never a reinterpretation.';

-- Parser identity stays all-or-nothing, now including the layout it answers to.
ALTER TABLE import_batch
    DROP CONSTRAINT import_batch_parser_is_complete;

ALTER TABLE import_batch
    ADD CONSTRAINT import_batch_parser_is_complete CHECK (
        (
            parser_id IS NULL AND parser_version IS NULL
            AND institution IS NULL AND document_family IS NULL AND layout_version IS NULL
        )
        OR (
            parser_id IS NOT NULL AND parser_version IS NOT NULL
            AND institution IS NOT NULL AND document_family IS NOT NULL
            AND layout_version IS NOT NULL
        )
    );

CREATE INDEX import_batch_layout_idx
    ON import_batch (institution, document_family, layout_version);
