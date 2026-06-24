# JedaKids Android App

JedaKids is a Kotlin and Jetpack Compose learning project for creating healthier
screen-time habits. A parent starts a timed session, Android preserves the
deadline outside the visible activity, and the device lock screen is shown when
the session expires.

This directory contains the Android Studio project.

## Features

The checked-in source implements:

- Kotlin application code;
- Jetpack Compose interface;
- session lengths from 1 to 480 minutes;
- exact Android alarm at the saved deadline;
- device-administrator screen locking;
- alarm restoration after reboot;
- local `SharedPreferences` persistence;
- unit tests for time calculations and formatting.

## Important limitation

This is a learning MVP, not a tamper-resistant parental-control product.

JedaKids calls `DevicePolicyManager.lockNow()`. A child who knows the device PIN
can unlock the device. A device user may also deactivate device administrator
access, revoke exact-alarm access, force-stop the app, change the system clock,
or uninstall the app.

Do not test JedaKids on a phone unless you know its unlock PIN. Use a dedicated
test device before relying on behavior outside the Android Emulator.

## Requirements

- Android Studio with the Android SDK and emulator;
- Android SDK platform 36.1 for the current project configuration;
- Android Studio's embedded JDK 21;
- an Android emulator or physical device running Android 8.0/API 26 or newer;
- Git for version control.

The project uses the Gradle wrapper, so a separate global Gradle installation
is not required.

## Open the project

1. Start Android Studio.
2. Choose **Open**.
3. Select this directory:

   ```text
   /Users/macbookpro/Documents/Playground/jeda-kids/android-app
   ```

4. Wait for Gradle sync and indexing to finish.
5. Keep Android Studio's **Gradle JDK** set to **Embedded JDK**.

## Build from Terminal

On macOS, point the shell to Android Studio's embedded JDK if no system Java is
installed:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
unset DYLD_LIBRARY_PATH
```

Build the debug APK:

```bash
cd /Users/macbookpro/Documents/Playground/jeda-kids/android-app
./gradlew assembleDebug
```

The generated APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run lint, unit tests, and the debug build together:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Run unit tests

From Terminal:

```bash
./gradlew testDebugUnitTest
```

The HTML test report is written to:

```text
app/build/reports/tests/testDebugUnitTest/index.html
```

In Android Studio, open
`app/src/test/java/com/jedakids/app/TimerMathTest.kt` and click the green test
triangle beside the class or an individual test.

## Run on an emulator

1. Open **Tools > Device Manager**.
2. Launch the virtual device named `JedaKids Test`.
3. Wait for the Android home screen.
4. Select `app` in Android Studio's run-configuration menu.
5. Select `JedaKids Test` in the target-device menu.
6. Click the green **Run 'app'** triangle.
7. Wait for Android Studio to build, install, and open JedaKids.

For reliable lock testing, configure a PIN inside the emulator under
**Settings > Security & privacy > Device unlock > Screen lock**. Use a test PIN
that is not a personal credential.

## Run on a physical Android phone

1. On the phone, open **Settings > About phone**.
2. Tap **Build number** seven times to enable Developer options.
3. Open Developer options and enable **USB debugging**.
4. Connect the unlocked phone to the Mac with a data-capable USB cable.
5. Accept the RSA debugging prompt on the phone.
6. Select the phone in Android Studio's target-device menu.
7. Click **Run 'app'**.

You can also install the generated debug APK with Android Debug Bridge:

```bash
$HOME/Library/Android/sdk/platform-tools/adb install -r \
    app/build/outputs/apk/debug/app-debug.apk
```

## Required on-device setup

JedaKids requires two special capabilities before a session can start.

### Device administrator

1. Open JedaKids.
2. Tap **Enable device administrator**.
3. Read the Android-owned confirmation screen.
4. Confirm that JedaKids requests the ability to lock the screen.
5. Tap **Activate this device admin app** or the equivalent confirmation.

### Alarms and reminders

1. Return to JedaKids.
2. Tap **Allow alarms & reminders**.
3. Enable **Allow setting alarms and reminders**.
4. Return to JedaKids.

Both setup cards should display **Enabled**.

## Manual smoke test

1. Configure a test screen-lock PIN.
2. Enable device administrator and exact-alarm access.
3. Tap **Lock now - test**.
4. Confirm Android displays the system lock screen.
5. Unlock the device with the test PIN.
6. Open JedaKids and start a one-minute session.
7. Leave JedaKids and open another app.
8. Wait until the deadline.
9. Confirm Android displays the lock screen again.

The current UI calculates the countdown from the saved deadline when the
activity is rendered. It does not redraw the countdown every second.

## Project structure

```text
android-app/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/jedakids/app/
│       │   │   ├── AlarmReceiver.kt
│       │   │   ├── JedaDeviceAdminReceiver.kt
│       │   │   ├── LockController.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── RecoveryReceiver.kt
│       │   │   ├── TimerMath.kt
│       │   │   ├── TimerPreferences.kt
│       │   │   └── TimerScheduler.kt
│       │   └── res/
│       └── test/java/com/jedakids/app/
│           └── TimerMathTest.kt
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Component responsibilities

| Component | Responsibility |
|---|---|
| `MainActivity` | Compose UI, permission requests, and session controls |
| `TimerMath` | Duration validation, remaining-time calculation, and formatting |
| `TimerPreferences` | Persisting the active session deadline |
| `TimerScheduler` | Scheduling and cancelling the exact alarm |
| `AlarmReceiver` | Handling the deadline and requesting the device lock |
| `RecoveryReceiver` | Restoring or completing a session after reboot or permission changes |
| `LockController` | Checking device-admin state and calling `lockNow()` |
| `JedaDeviceAdminReceiver` | Receiving device-administrator lifecycle callbacks |

## Troubleshooting

### Terminal cannot locate Java

Run:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

### Gradle reports `ZIP_Open` or `libzip.dylib`

Remove any global `DYLD_LIBRARY_PATH` export from the shell startup file, open
a new Terminal, and run:

```bash
unset DYLD_LIBRARY_PATH
./gradlew --stop
./gradlew testDebugUnitTest assembleDebug
```

### The phone or emulator does not appear in Android Studio

Open **Tools > Troubleshoot Device Connections**. For a phone, confirm USB
debugging is enabled and accept the RSA prompt. For an emulator, wait until it
has reached the Android home screen.

### The app cannot be uninstalled

Android may prevent uninstalling an active device-administrator app. Open the
phone's device-administrator settings, deactivate JedaKids, and then uninstall
it normally.

### The deadline does not lock the device

Confirm that both setup cards display **Enabled**, then use **Lock now - test**
to check device-administrator behavior independently from alarm scheduling.

## Release workflow

To create the initial source commit:

```bash
git add .
git commit -m "feat: add JedaKids MVP"
```

The debug APK is suitable for private development testing. Do not treat it as a
production release or distribute it publicly as a parental-control product.
