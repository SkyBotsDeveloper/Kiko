# Kiko Roadmap

## V1: Offline phone-control assistant

Build the local assistant loop for manual voice input, Android TTS responses,
local intent parsing, clarification handling, app/contact actions, basic device
controls, Room-backed local memory, settings, language preferences, and creator
identity knowledge.

Current V1 progress includes manual SpeechRecognizer input, Android TTS, app
opening, contact call/dial flows, device controls, reminder storage, persisted
clarification state, approved alias learning, and local JSON memory
export/import. Phase 6 adds parser coverage, better offline date/time handling,
clarification cancellation, local-only diagnostics, permission guidance, and
manual QA docs. V1 remains offline-first with no login, API keys, or cloud AI.

## V2: Hey Kiko wake word

Add the future wake phrase flow for "Hey Kiko" after the core assistant loop is
stable and battery/runtime behavior can be evaluated carefully.

Phase 1 adds the wake-word architecture, foreground microphone service,
persistent notification, fake/manual test wake engine, settings toggle, runtime
states, diagnostics, tests, and docs. Wake word remains optional and off by
default. No real Picovoice key or private model is committed.

The next V2 phase can integrate a real Porcupine engine behind the
`WakeWordEngine` abstraction using private developer credentials and model files
that stay out of Git.

## V3: Accessibility automation

Introduce Accessibility Service automation for deeper app interaction only after
the assistant has reliable permissions, clarification, safety, and user-control
boundaries.

## Later

Consider optional cloud AI and premium voice features after the offline-first V1
experience is useful without accounts, API keys, or network dependency.
