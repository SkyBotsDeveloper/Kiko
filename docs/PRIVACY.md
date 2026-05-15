# Kiko Privacy

Kiko V1 is local-first.

- No login system.
- No cloud AI in V1 or the V2 wake-word foundation.
- No API keys.
- Wake word is optional and off by default in V2.
- Wake-word audio is not sent to cloud services.
- Kiko does not use Picovoice Porcupine, Picovoice AccessKeys, or `.ppn` /
  `.pv` files.
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

Kiko does not send contacts, app lists, reminders, aliases, preferences,
assistant memory, or wake-word audio to a server.

Memory export does not include raw full conversations, hidden API keys, secrets,
or a full contacts dump. Contact aliases are saved only after user-approved
personalization decisions.

Real wake-word detection requires a trained local TFLite model in a later
phase. Private training data, generated model artifacts, and non-redistributable
models must stay out of the repository.
