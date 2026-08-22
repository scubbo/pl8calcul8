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

No app store, no manual USB cable.

## Server deployment (automatic up to a PR)

```
push to main (server/shared/gradle changes)
        │
        ▼
GitHub Actions (.github/workflows/server-image.yml)
  job 1: builds ghcr.io/scubbo/pl8calcul8:sha-<commit>
  job 2: opens a PR in homelab-configuration bumping the tag in
         charts/pl8calcul8/values.yaml (as UpdaterBot)
        │
        ▼
Jack approves + merges the PR (homelab main is protected)
        │
        ▼
ArgoCD notices the merge and rolls the deployment
```

Job 2 authenticates with the same GitHub App used by blog-content's
deploy automation (client-id `Iv23linrv3EjCnSWuWoS`). It needs:

* repo secret `DEPLOYMENT_APP_PRIVATE_KEY` (same PEM as in blog-content's
  secrets): `gh secret set DEPLOYMENT_APP_PRIVATE_KEY --repo scubbo/pl8calcul8 < key.pem`

## Backup tracks (multi-user)

The server supports multiple isolated backup "tracks", one per token,
configured via `BACKUP_TOKENS=name:token,name:token` (each track's
backups live in `$DATA_DIR/<name>/`). The legacy `BACKUP_TOKEN=<token>`
form still works as a single track named `default`; on startup, any
pre-track backups found loose in `$DATA_DIR` are moved into the first
configured track's directory.

To add a user: add `name:token` to the `backup-tokens` key of the
`pl8calcul8-secrets` k8s Secret (see the chart's `secret.example.yaml`)
and restart the deployment. Track names become directory names:
letters, digits, `_`, `-` only.
* the App's installation to include `homelab-configuration`
  (github.com → Settings → Integrations → the deployment app → Repository
  access). If it's scoped to selected repositories, add it there.

## Repo protection

`main` is protected by the `protect-main` ruleset: all changes require a
PR with one approval; force-pushes and deletions are blocked; repository
admins (Jack) may bypass. Manage at
https://github.com/scubbo/pl8calcul8/rules or `gh api /repos/scubbo/pl8calcul8/rulesets`.

## Cutting a release

**The normal path: merge a PR to main.** Every merged PR is tagged and
released automatically by `.github/workflows/auto-tag.yml`. Control the
version bump with a PR label (set it any time before merging):

| Label           | Effect on latest `vX.Y`      |
|-----------------|------------------------------|
| *(none)*        | minor bump: `v0.4` → `v0.5`  |
| `release:minor` | same as above, explicit      |
| `release:major` | major bump: `v0.4` → `v1.0`  |
| `release:skip`  | no tag, no release           |

Implementation note: the auto-tag workflow pushes the tag with
GITHUB_TOKEN, and GitHub deliberately does NOT trigger `push`-on-tag
workflows from such pushes (recursion guard). So auto-tag.yml also
explicitly dispatches release.yml with the new tag as its ref —
that's why release.yml has a `workflow_dispatch` trigger.

**The manual path** still works (e.g. re-releasing an old commit):

```bash
git tag v0.3        # version name = tag without the "v"
git push origin v0.3
```

Either way, the release workflow:

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
