-- Release 0.1 database foundation.
--
-- This migration deliberately creates no domain tables and inserts no seed data.
-- Flyway creates the managed schema before executing this script. Future schema
-- changes must be added as new immutable versioned migrations.

COMMENT ON SCHEMA personal_finance IS
    'Application-owned schema managed exclusively through Flyway migrations.';
