# Releasing & app updates

How pl8calcul8 gets from a git commit onto the phone, and how to operate
the pipeline. Written to be rediscoverable after months away.

## The big picture

```
git tag v0.3 && git push --tags
        │
        ▼
GitHub Actions (.github/workflows/release.yml)
  builds a SIGNED release APK, attaches it to a GitHub Release
        │
        ▼
Obtainium (app on the phone) watches github.com/scubbo/pl8calcul8,
  notifies when a new Release appears, downloads + installs the APK
```

No app store, no manual USB cable. The server component is deployed
separately (GitHub Actions builds the Docker image on push to main;
bump the image tag in homelab-configuration's chart to deploy).

## Cutting a release

```bash
git tag v0.3        # version name = tag without the "v"
git push origin main --tags
```

That's it. The workflow:

* computes `versionCode` as the commit count (`git rev-list --count HEAD`)
  — this must always increase or Android refuses the update, and commit
  count on a forward-only main branch always increases;
* sets `versionName` from the tag (`v0.3` → `0.3`);
* signs the APK with the release keystore (from repo secrets);
* creates a GitHub Release named after the tag with the APK attached
  and auto-generated notes.

Watch it at: https://github.com/scubbo/pl8calcul8/actions

## Signing: the part that bites

Android only installs an update over an installed app when both are
signed by the SAME key. Implications:

* The release keystore (`release.keystore`, gitignored) and its password
  live in Jack's password manager. **Losing them means the app can never
  be updated in place again** — only uninstall (losing local data unless
  backed up) and reinstall with a new key.
* GitHub Actions has copies as repo secrets:
  * `RELEASE_KEYSTORE_B64` — `base64 -i release.keystore`
  * `RELEASE_KEYSTORE_PASSWORD` — the keystore/key password
  (Set via `gh secret set NAME` or repo Settings → Secrets → Actions.)
* Debug builds (`./gradlew installDebug`, what the emulator uses) are
  signed with the machine-local debug key and CANNOT update a
  release-signed install (or vice versa). The phone runs release builds;
  the emulator runs debug builds. Don't cross the streams — installing
  one over the other requires an uninstall.

## Phone setup (new phone, or after a reinstall)

1. Install Obtainium: https://github.com/ImranR98/Obtainium/releases
   (download the APK on the phone, or `adb install` it).
2. In Obtainium: Add App → URL `https://github.com/scubbo/pl8calcul8`.
   It finds the latest Release and installs the APK.
3. In pl8calcul8 Settings: enter the backup server URL
   (`https://pl8calcul8.scubbo.org`) and token (password manager),
   then Restore if there's a backup to recover.

Updates thereafter: Obtainium notifies when a new Release exists;
tap to install. (Obtainium's "BG updates" can automate this further.)

## Building a signed release locally (rarely needed)

```bash
export RELEASE_KEYSTORE="$PWD/release.keystore"
export RELEASE_KEYSTORE_PASSWORD='<from password manager>'
./gradlew :app:assembleRelease -PversionCode=<N> -PversionName=<X.Y>
# output: app/build/outputs/apk/release/app-release.apk
```

Without those env vars, `assembleRelease` produces an UNSIGNED APK that
phones will refuse to install.

## Troubleshooting

* **"App not installed" / signature error on the phone**: the APK's
  signing key doesn't match the installed app (see Signing above).
  Back up from within the app, uninstall, install the new APK, restore.
* **Update refused with no error**: versionCode probably didn't increase.
  Check the Release's APK: `aapt dump badging pl8calcul8-X.Y.apk | head -1`.
* **Workflow fails at "Decode release keystore"**: repo secrets missing
  or mangled; re-set them (see Signing).
