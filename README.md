# Auto wallpapers

Auto wallpapers is an Android wallpaper changer derived from Paperize and customized for reliable operation on Android 12+ devices, including Honor phones.

## What changed in this version

This version contains a background scheduling redesign. Wallpaper changes are triggered by a self-rescheduling `AlarmManager` receiver rather than depending on the Activity or the user reopening the application. Exact alarms are used when Android grants the permission; otherwise the app uses an idle-safe inexact alarm so the schedule still has a background trigger. The receiver starts the wallpaper operation even when the app has been removed from the recent-apps list.

Unlock changes are handled directly from `USER_PRESENT`. The next wallpaper URI is applied from the receiver without opening the Activity, and the foreground service is retained only as a fallback. The fast unlock path intentionally skips expensive bitmap effects at that instant; normal scheduled changes continue to use the full rendering pipeline.

The Library `+` button now opens the Android folder picker directly. The selected folder becomes a new album automatically, using the folder name and importing its images without an intermediate album-name dialog or a second navigation step. Duplicate album names receive a numeric suffix automatically.

The main wallpaper screen keeps only the requested controls visible: **Change on unlock**, **Lock Album**, **Home Album**, and **Current wallpaper**. Scheduling, changer activation, intervals, scaling, shuffle, visual effects, and other controls are inside the expandable **Advanced** card.

## Honor setup required for reliable background operation

On Honor/MagicOS, enable the following for Auto wallpapers:

1. Allow the application to start automatically in the system app-launch or auto-start settings.
2. Set battery usage to unrestricted and keep the existing battery-optimization exemption enabled.
3. Grant the exact-alarm access requested when enabling wallpaper changes.
4. Do not use Android **Force stop** for testing. Force stop intentionally blocks broadcasts, alarms, and background restarts for ordinary applications. Removing the app from recent apps is the correct test for background execution.

No Android application can bypass a deliberate Force stop or a device shutdown. OEM auto-start and battery-management policies remain system controls.

## Build requirements

| Requirement | Version |
|---|---:|
| Java | 17 |
| Minimum Android SDK | 31 |
| Compile/Target SDK | 36 |
| Build system | Gradle wrapper included |

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleRelease
```

The release APK built for this repository is available at [`releases/auto-wallpapers-v4.apk`](releases/auto-wallpapers-v4.apk).

## Verification

The final release passed `assembleRelease`, `lintRelease`, `assembleDebug`, `lintDebug`, and `testDebugUnitTest`. The APK is signed with one signer and verifies using APK Signature Scheme v2.

SHA-256:

```text
4792b0947b728f8b7cf4f814fd867391ea52d0027a7867b8a44c1900a9b7fdaf  auto-wallpapers-v4.apk
```

## License

The upstream project is licensed under GNU GPL v3.0. See [`LICENSE`](LICENSE).
