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
7. After the least-privilege roles exist, run version 4 as
   `myfitness_migrator` with `SET ROLE myfitness_owner`. V4 creates the durable
   TrainingSession idempotency table and establishes the complete minimum
   application grants: `users` read/create, TrainingSession aggregate CRUD,
   and only their required sequences. No legacy-feature table DML is granted.
8. Run `flyway:validate` and start the application only after the ownership
   and idempotency entity changes are ready.

## Empty database

Create the documented owner, migrator, and application roles first, then run
all migrations through version 4. V1 creates the baseline schema, V2 adds
the nullable ownership column and constraints, and V3 can make the empty
column non-null without a backfill. V4 must run with
`SET ROLE myfitness_owner` and requires `myfitness_app` to exist.

Idempotency records are retained indefinitely for the MVP. Deleting a Session
sets the record's Session reference to null, but does not make its key reusable.
Define and test an explicit retention policy before adding automated cleanup.

Never commit a user-specific backfill or a local Flyway configuration file.
