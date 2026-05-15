# Kiko V1 Manual QA

Use a real Android device when possible. Keep V1 offline-first: do not add cloud
AI, wake word, Accessibility Service, API keys, or login flows.

## Setup

- Install the debug APK.
- Grant microphone permission only.
- Tap the mic button and confirm one-shot listening starts and stops.
- Confirm Android TTS speaks a Kiko response when voice replies are enabled.
- Grant contacts only when a contact/call action asks for it.
- Test dialer fallback when direct call permission is missing.
- Create a reminder and grant notification permission only when requested.
- Open Settings and test the optional system brightness permission flow.

## Voice And Action Tests

- Say `Open Telegram`.
- Say `yt open karo`.
- Say `Call mummy` for a single contact.
- Say `Call mummy` when multiple mummy contacts exist.
- Resolve contact clarification by saying the full candidate name.
- Test a contact with multiple numbers and choose `Mobile` or `Home`.
- Say `Torch jalao`, then `torch bujha do`.
- Say `Volume 50`.
- Say `Volume badhao`, then `volume kam karo`.
- Say `Brightness 70`.
- Say `Kal 6 baje alarm lagao`.
- Say `Remind me at 8 PM to study`.
- Ask `who created you`.
- Say `weather batao` while offline and confirm Kiko returns the offline V1
  internet-required message.

## Memory Tests

- Resolve an ambiguous contact and approve remembering the alias.
- Reuse the learned contact alias.
- Turn personalization off and confirm Kiko stops saving new aliases.
- Clear local memory from Settings.
- Export memory JSON and confirm it has no raw full conversations or contact
  dumps.
- Import memory JSON and confirm preferences/aliases are restored.

## Regression Tests

- Rotate the screen if the device supports rotation.
- Trigger app process recreation if possible and confirm pending clarification
  expiry/restore behaves safely.
- Deny microphone permission and confirm there is no crash.
- Deny contacts permission and confirm contact lookup asks gracefully.
- Confirm no crash when contacts are missing or empty.
- Confirm no crash if flashlight hardware is unavailable or camera is busy.
- Confirm no crash if Android TTS language support is missing.
- Confirm reminders are stored even if exact notification delivery is delayed by
  Android or OEM battery policy.
