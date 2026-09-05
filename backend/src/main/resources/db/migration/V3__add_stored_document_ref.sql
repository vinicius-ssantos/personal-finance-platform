-- Links an import to its ephemeral source document (issue #7).
--
-- The column holds an opaque handle, never a filesystem path: FR-UPLOAD-005
-- forbids exposing the real path, and the application never stores one.
-- Nullable because the handle is cleared once the file is purged, while the
-- batch and its audit trail remain.

ALTER TABLE import_batch
    ADD COLUMN stored_document_ref UUID;

COMMENT ON COLUMN import_batch.stored_document_ref IS
    'Opaque handle to the ephemeral source document. Never a filesystem path. Null once purged.';
