# FocusDay — cute Android task planner

This is the complete Android Studio / GitHub Actions source project. The ZIP is arranged in the Android project structure; **do not move the files out of their folders**.

## Project layout

- `settings.gradle.kts` and `build.gradle.kts` — project-level Gradle configuration
- `app/build.gradle.kts` — Android app configuration
- `app/src/main/AndroidManifest.xml` — app icon and Android components
- `app/src/main/res/mipmap-*/ic_launcher.png` — kitten launcher icon at each Android density
- `app/src/main/res/mipmap-*/ic_launcher_round.png` — round kitten launcher icon at each Android density
- `app/src/main/res/drawable-nodpi/welcome_cat.jpg` — welcome illustration used by `MainActivity.kt`
- `app/src/main/java/com/focusday/app/` — Kotlin app code
- `.github/workflows/android.yml` — GitHub Actions build workflow

## Build an APK on GitHub

1. Download and extract this ZIP.
2. In your GitHub repository, upload the **contents of the extracted folder** to the repository root. The root should show `app`, `.github`, `settings.gradle.kts`, `build.gradle.kts`, and `gradle.properties`.
3. Do not upload `app/` inside another extra `FocusDay_Cute_v2/` folder.
4. Open **Actions → Build FocusDay APK → Run workflow**.
5. When the run succeeds, open its summary, download `FocusDay-debug-apk`, and extract `app-debug.apk`.

The icon and welcome image are already connected in the source: the manifest uses the mipmap launcher icons, and `MainActivity.kt` loads `R.drawable.welcome_cat`.

## Included features

- Cute kitten launcher icon and pastel welcome image
- Daily task list, completion toggles, and elapsed-time progress bar
- Current-task notification and task start/end reminders
- Animated walking-cat live wallpaper service
- GitHub Actions workflow to build a debug APK

## Important notes

- The task schedule is a sample schedule hardcoded in `TaskData.kt`.
- Android 13+ may ask for notification permission. Enable FocusDay notifications for reminders.
- Samsung/Android battery settings may delay scheduled alarms.
- The walking cat is an Android live wallpaper, not a cat overlay on top of the existing lock screen. Whether it can be set on the lock screen depends on the phone's One UI version.
- A debug APK built on a fresh GitHub runner may not install as an update over an older debug APK because the signing key can differ. If Android says “App not installed”, uninstall the older FocusDay app first, then install the new APK. Uninstalling clears the app's locally stored task completion states.
