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

Out of scope for V1:

- Cloud AI dependency
- API keys
- Login system
- Wake word implementation
- Accessibility Service automation
