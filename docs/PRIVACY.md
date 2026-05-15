# Kiko Privacy

Kiko V1 is local-first.

- No login system.
- No cloud AI in V1.
- No API keys.
- Contacts stay on device.
- Installed app lists stay on device.
- Memory is stored locally in Room.
- Full raw conversations are not stored by default.
- Optional interaction summaries are structured and minimal.
- User-approved aliases can be saved for app/contact personalization.
- Users can clear local memory from settings.
- JSON export/import is local and user-controlled.
- Diagnostics are local Logcat-only messages.
- Diagnostics redact phone-like numbers and truncate long text.

Kiko does not send contacts, app lists, reminders, aliases, preferences, or
assistant memory to a server in V1.

Memory export does not include raw full conversations, hidden API keys, secrets,
or a full contacts dump. Contact aliases are saved only after user-approved
personalization decisions.
