# Relisten Car — Android Automotive OS app

A self-contained, pure-Kotlin Android Automotive OS (AAOS) media app for streaming live
concert recordings from [relisten.net](https://relisten.net) / archive.org, built as part of
this fork. It is **not** part of the Expo/React Native phone app — on AAOS, media apps expose
a `MediaLibraryService` browse tree and the car's own Media Center renders all UI (the same
model as CarPlay), so no React Native code runs in the car.

Default landing tab is **Tedeschi Trucks Band**, with an **All Artists** tab for the full
Relisten catalog and an **On This Day** tab.

- `applicationId`: `com.ttblisten` (deliberately distinct from the official
  `net.relisten.android`; this is an unofficial private-distribution fork — the package
  name is registered to the owner's Play developer account)
- Stack: Media3 (ExoPlayer + MediaLibrarySession), OkHttp, kotlinx.serialization, DataStore
- Browse hierarchy mirrors the iOS CarPlay design (`relisten/carplay/`), and the playback
  service is modeled on `modules/relisten-audio-player/android`
- Source auto-selection ports `sortSources` from `relisten/realm/models/show_repo.ts`
  (soundboard > famous tapers > weighted rating) so no tape-choosing happens while driving

## Build

```
cd automotive
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Run on an Automotive AVD (e.g. `system-images;android-33;android-automotive;arm64-v8a`),
then open the car's **Media** app and pick "Relisten Car" from the source switcher.

Licensed AGPL-3.0, same as the rest of this repository.
