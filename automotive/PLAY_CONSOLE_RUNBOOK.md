# Play Console runbook — private install on the Polestar 2

Follow these steps in order. Total hands-on time ≈ 30–45 min, plus Google identity
verification (can take a few days) on step 1 — start that early.

## 1. Create the Play developer account (one-time, $25)

1. Go to <https://play.google.com/console/signup>.
2. **Use the same Google account that is signed into your Polestar 2** (check in the car:
   Settings → Google → your account). This makes tester opt-in and car install seamless.
3. Choose "Yourself" (personal account), pay the $25 one-time fee, and complete identity
   verification. New personal accounts must also complete a small closed test **only if you
   later go to production** — internal testing (this runbook) is not gated by that.

## 2. Create the app

1. Play Console → **Create app**.
   - Name: `Relisten Car (unofficial)` (anything works; it's private).
   - App or game: App. Free.
2. Complete the **App content** declarations (Dashboard → App content):
   - Privacy policy: link to this fork on GitHub:
     <https://github.com/dclfareguru/relisten-mobile/blob/automotive/automotive/README.md>
   - Ads: No. Data safety: no data collected/shared (the app calls api.relisten.net and
     archive.org anonymously; there are no accounts, no analytics).
   - Content rating questionnaire: Music/streaming answers, no user content.

## 3. Opt in to the Android Automotive OS form factor

1. **Test and release → Setup → Advanced settings → Form factors** tab.
2. **Add form factor → Android Automotive OS**. This unlocks the dedicated
   "Android Automotive OS only" release tracks.

## 4. Upload to the Automotive internal testing track

1. **Testing → Internal testing** → make sure you're on the **Android Automotive OS
   only** track (track selector at top) → **Create new release**.
2. Accept Play App Signing (default). Upload
   `automotive/app/build/outputs/bundle/release/app-release.aab`.
   - Rebuild any time with:
     `cd automotive && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease`
   - **Bump `versionCode`** in `automotive/app/build.gradle.kts` for every new upload.
3. Release name/notes: anything. **Save and publish** (internal testing publishes without
   Google review; the car-app quality review only applies to closed/open/production).
4. **Testers** tab → create an email list containing `eking@dynamixwebdesign.com`
   (and any family accounts) → save.
5. Copy the **"How testers join your test"** opt-in URL, open it in a browser while signed
   in as that Google account, and click **Accept invitation**.

## 5. Install in the Polestar 2

1. In the car (parked, good connectivity): app grid → **Play Store** (Google account must
   match the tester email).
2. Search **"Relisten Car"** — internal-testing apps appear for opted-in testers.
   First-time propagation can take from a few minutes up to a couple of hours; if it
   doesn't show, reboot the infotainment (hold the home button) and check again.
3. Install → open the **Media** app (or the app icon) → the Tedeschi Trucks tab is the
   default landing view. Play something and drive.

## 6. Keeping it updated

- Code change → bump `versionCode` → `./gradlew :app:bundleRelease` → new release on the
  same internal track → the car's Play Store auto-updates (or update manually).
- The upload keystore is at `~/keystores/relisten-car-upload.jks`; its passwords are in
  `~/.gradle/gradle.properties` (`RELISTEN_CAR_UPLOAD_*`). Play App Signing holds the real
  signing key, so a lost upload key is recoverable via Play Console support.

## AGPL-3.0 note

relisten-mobile is AGPL-3.0. Distributing this build (even privately via internal testing)
is distribution — keep this fork **public** on GitHub with the `automotive/` sources, and
put the repo URL in the Play listing description. To publish the fork:

```
gh auth login                      # one-time browser auth
gh repo fork RelistenNet/relisten-mobile --remote
git push origin automotive
```

(The local repo already has RelistenNet as the `upstream` remote and all work is on the
`automotive` branch.)
