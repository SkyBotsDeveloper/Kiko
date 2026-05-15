# Database Migration Policy

Kiko V1 is still pre-release. The Room database schema is versioned and schema
export is enabled so changes can be reviewed before public release.

## Current Pre-release Policy

- Room uses schema version `1`.
- Destructive migration is currently allowed only because V1 is not public yet.
- This can wipe local memory during development builds if the schema changes.
- The limitation is acceptable for pre-release testing, but it must not be used
  silently for public user data.

## Public Release Policy

Before a public V1 release:

- Treat the Room schema as stable.
- Remove destructive migration for public builds.
- Add explicit Room migrations for each schema version change.
- Keep schema export committed for review.
- Test migration paths with real local memory data.

## User Backup

Kiko's local JSON export can help users back up preferences, aliases, and
reminders before major storage changes. Export/import is user-controlled and is
not a replacement for proper Room migrations after public release.
