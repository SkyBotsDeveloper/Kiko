# Kiko V1 Scope

Kiko V1 is an offline-first Android phone-control assistant, not a chatbot.

Planned V1 features:

- Manual mic input
- Android SpeechRecognizer
- Android TextToSpeech
- Local intent parser
- Open installed apps
- Call contacts
- Clarification handling
- Flashlight, volume, brightness, alarm, and reminder basics
- Local memory
- Language mirroring
- Creator identity knowledge

Phase 2 foundation completed:

- Permission status foundation for microphone, contacts, phone calls, and camera
- Manual microphone request flow
- SpeechRecognizer wrapper for one-shot manual voice input
- TextToSpeech wrapper for local spoken responses
- AssistantOrchestrator skeleton
- Local intent parser contract and basic parser
- Safe action handler contracts and placeholder stubs

Phase 3 foundation completed:

- Installed app detection through launchable PackageManager queries
- Fuzzy app matching with common aliases such as insta, ig, wa, yt, and chrome
- Real app launch flow with graceful failure responses
- Permission-aware contacts repository using ContactsContract
- Fuzzy contact matching for names and relation-style labels
- Contact call flow with ACTION_CALL when permitted and ACTION_DIAL fallback
- In-memory clarification handling for ambiguous app/contact matches
- Local English, Hinglish, and Hindi response selection for app/contact actions
- No cloud dependency for app opening or contact calling

Phase 4 foundation completed:

- Flashlight on/off support through CameraManager with unavailable/error handling
- Media volume set/increase/decrease support through AudioManager
- Brightness set/increase/decrease support with app-window fallback when system write settings is unavailable
- Alarm creation through Android AlarmClock intents
- Local reminder storage and inexact notification scheduling when notification permission is available
- Multi-number contact clarification for contacts with Mobile/Home/etc. numbers
- Device-control responses in English, Hinglish, and Hindi

Phase 5 foundation completed:

- Room DB foundation for structured local memory
- Stored user preferences for language style, reply style, voice replies,
  personalization, and optional interaction summaries
- Local app/contact alias memory after user-approved clarification outcomes
- Pending clarification persistence with expiry for short process recreation
- Reminder storage integrated with Room while keeping inexact scheduling limits
- Settings screen for preferences, local memory clear/export/import, brightness
  permission affordance, notification status, and About Kiko
- JSON memory export/import that excludes secrets, raw contact dumps, and full
  raw conversation history
- Improved language/style preference routing across app, contact, device,
  creator, unknown, internet-required, permission, and memory responses

Out of scope for V1:

- Cloud AI dependency
- API keys
- Login system
- Wake word implementation
- Accessibility Service automation
