# Session handoff notes

State snapshot for resuming development after machine restart.
Last updated: 2026-08-16.

## Current state

* All committed through `c8eb60f` on `main`. Working tree should be clean
  except this file and REQUIREMENTS.md edits (commit them).
* 18 JVM unit tests + 5 instrumented DB tests, all passing.
* V1 SHIPPED: installed on Jack's Pixel 10a (debug build via adb).
  Backup verified against the real server with Jack's token. Launcher
  icon, IME-resize fix, and password-type token field all landed.
* Redeploying app updates to the phone: plug in via USB, then
  `./gradlew assembleDebug && adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
  (BUT: moving to release-signed builds via Obtainium — see docs/RELEASING.md)
* Release pipeline LIVE: v0.2 on the phone via Obtainium. Operating
  guide: docs/RELEASING.md.
* Web UI shipped (server / endpoint + auth'd /history). Deploy
  automation: server image builds open a PR in homelab-configuration
  (its main is protected); merging deploys via ArgoCD.
* pl8calcul8 main is protected (ruleset protect-main): PRs + 1 approval
  required, admin (Jack) bypasses. Agent pushes from this machine count
  as Jack - push only when instructed.
* WARNING: never run connectedAndroidTest with the phone plugged in —
  it uninstalls the app from ALL devices (wiped Jack's phone once).
  Always ANDROID_SERIAL=emulator-5554.
* Backlog in REQUIREMENTS.md (rest timer, CSV export, dark mode,
  session survives process death). Also consider: automatic backups,
  backup timestamp shown in Settings.
* Emulator UI automation tip: don't tap blind coordinates (dialogs shift
  when the keyboard opens). Use `/tmp/tap.py`-style uiautomator dumps to
  find widget bounds by text, or re-dump after IME changes.

## Environment / commands

* Build JDK is Android Studio's bundled JBR (JDK 25). Every gradle command
  needs: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
* SDK at `~/Library/Android/sdk` (referenced by `local.properties`, which is
  gitignored — recreate with `sdk.dir=/Users/scubbo/Library/Android/sdk` if lost).
* Start emulator — the `-feature -Vulkan` flag is REQUIRED on this machine:
  `~/Library/Android/sdk/emulator/emulator -avd pl8phone -feature -Vulkan &`
  Without it, Android 16's UI renders Vulkan-on-SwiftShader (software):
  frames take seconds, apps ANR ("Input dispatching timed out" while main
  thread is Runnable inside Compose draw), and the whole Mac slows down.
  With the flag, rendering falls back to host-accelerated OpenGL.
  (AVD `pl8phone` = Pixel 7, Android 16/API 36, x86_64. `hw.keyboard=yes`
  was set in `~/.android/avd/pl8phone.avd/config.ini` so the Mac keyboard
  types into the emulator.)
* Install + launch app:
  `./gradlew installDebug && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.scubbo.pl8calcul8/.MainActivity`
* Unit tests: `./gradlew testDebugUnitTest`
* On-device tests: `./gradlew connectedDebugAndroidTest`
  **WARNING**: this uninstalls the app (and its database) afterward —
  run `installDebug` again after.

## Deployment state

* Server is LIVE: pl8calcul8.scubbo.org (Cloudflare tunnel) and
  pl8calcul8.avril (internal). Chart in homelab-configuration
  charts/pl8calcul8, image ghcr.io/scubbo/pl8calcul8 pinned by sha tag.
  Deploys = GitHub Actions builds on push to main, then bump the tag in
  values.yaml.
* Bearer token lives in the pl8calcul8-secrets k8s Secret (Jack holds it).

## Gotchas discovered

* Querying a brand-new .avril hostname before external-dns registers it
  makes Unbound negative-cache the NXDOMAIN (root SOA, up to ~1h). Flush
  Unbound or wait; the record itself is fine.

* AGP 9 has built-in Kotlin: do NOT apply `org.jetbrains.kotlin.android`
  (build fails). `org.jetbrains.kotlin.plugin.compose` and KSP still apply.
* Jack's global gitignore excludes `gradle/`, `gradlew`; repo .gitignore
  overrides with `!gradle/` etc. If a gradle-file commit is mysteriously
  missing files, check ignores.
* Emulator on this Intel Mac is slow to cold-boot and throws harmless
  system ANR dialogs ("Process system isn't responding") — hit Wait.
* Don't launch a second emulator before the first fully exits
  (single-AVD lock).
* Latest-version lookups: query
  `https://dl.google.com/android/maven2/<group>/<artifact>/maven-metadata.xml`
  (AndroidX/AGP) or Maven Central metadata (Kotlin/KSP) instead of trusting
  training data. Current pins in `gradle/libs.versions.toml`.
* Ollama isn't running on this machine, so hivemind memory store fails —
  persist notes to files instead.

## Jack's ideas for next session (2026-08-17, in his words + notes)

1. **Auto-merge automation PRs in homelab-configuration** when
   "cryptographically signed as being from this automation".
   Design note: commits pushed via plain git from Actions are NOT signed.
   But commits created via the GitHub API are automatically GPG-signed by
   GitHub with the App as author. So: switch the deploy job to create its
   commit via API (not git push), then a homelab-configuration ruleset/
   workflow can require verified signatures + author == deployment app
   and auto-approve/merge only those PRs.
2. **Track bodyweight** (manual entry, or pull from Google Health -
   on Android that's the Health Connect API) as an input to...
3. **...Symmetric Strength-style calculations**: strength scores per
   lift normalized by bodyweight/sex/age, strength-level classifications,
   and muscle-balance ratios between lifts. Needs research into their
   formulas (allometric scaling) and standards tables - find or derive
   before building. Depends on (2) for bodyweight.

## Design decisions (full detail in REQUIREMENTS.md)

* RPE chart embedded in `RpeCalculator.kt`, verified by Jack; RPE 6 column
  derived via diagonal, 12@6 extrapolated 57.4%.
* Assigned RPE integers 6-10; recorded RPE half-steps 6.5-10.
* Weight advice: most recent exercise -> e1RM -> target + per-lift
  increment -> round to nearest 5, ties down.
* Exercise = one weight+RPE per exercise (not per set); reps assumed
  as assigned; notes for deviations.
