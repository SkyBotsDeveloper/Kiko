# Android Limitations

Kiko V1 stays offline and uses Android platform APIs directly. Some phone-control
actions are limited by Android permissions, device hardware, and OEM behavior.

## Brightness

Kiko can adjust its own app window brightness without special permission.
System-wide brightness changes require Android write-settings access
(`Settings.System.canWrite`). If that access is missing, Kiko uses the app-window
fallback and tells the user that full phone brightness needs extra permission.

## Alarms

Basic alarm creation uses Android's `AlarmClock` intent. This avoids exact-alarm
permission for the V1 flow, but the final UI and behavior depend on the device's
installed clock app.

## Reminders

Kiko stores reminders locally. Reminder notification scheduling uses an inexact
`AlarmManager` flow, so exact delivery is not guaranteed. On Android 13 and newer,
notification permission is required before Kiko can alert the user.
Reminder records are stored in Room and delivery status is updated when the
notification receiver can run, but Android can still delay or suppress alarms
based on battery, standby, OEM policy, or missing notification permission.

## Flashlight

Flashlight control uses `CameraManager.setTorchMode`. Some devices may not have
flash hardware, and the torch can be unavailable if the camera is in use or the
OEM camera service rejects the request. Kiko handles those cases without crashing.

## Local Memory

Room is the primary V1 storage layer for preferences, aliases, pending
clarifications, reminders, and optional structured summaries. Because Kiko is
pre-release, destructive schema migration is currently enabled and documented so
the local schema can stabilize before a public V1 build.
