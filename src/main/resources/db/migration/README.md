# Flyway migration sequence

Flyway is disabled in `application.yml` while existing databases are being
baselined. Do not enable automatic migration until every environment has a
`flyway_schema_history` table and has completed the sequence below.

## Existing database

1. Verify the backup and current row counts.
2. Baseline the existing schema at version 1. `V1__baseline_schema.sql` must not
   be executed against an existing schema.
3. Migrate only through version 2.
4. Run the separately stored, local-only owner backfill SQL.
5. Verify that no `training_sessions.user_id` value is null and that the
   Session, Exercise, and Set counts are unchanged.
6. Migrate through version 3.
7. Run `flyway:validate` and start the application only after the ownership
   entity changes are ready.

## Empty database

Run all migrations through version 3. V1 creates the baseline schema, V2 adds
the nullable ownership column and constraints, and V3 can make the empty
column non-null without a backfill.

Never commit a user-specific backfill or a local Flyway configuration file.
