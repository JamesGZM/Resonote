# Android release signing

This directory is the local home for Resonote release-signing material.

Release certificate SHA-256:
`84:DF:8A:D7:C7:F0:75:96:DC:C9:0E:CD:C1:C5:3D:CC:15:F3:D6:AD:8A:AC:B8:B5:54:EB:54:20:D7:4B:AD:68`

- `resonote-release.jks` and `signing.properties` are intentionally ignored by Git.
- Back up the keystore and its credentials in a secure location. Losing them prevents future APK updates with the same application ID.
- Never commit, upload as a build artifact, or paste the populated properties into logs.
- Local release build: `./gradlew :app:assembleRelease`.
- CI may provide `RESONOTE_KEYSTORE_PATH`, `RESONOTE_KEYSTORE_PASSWORD`, `RESONOTE_KEY_ALIAS`, and
  `RESONOTE_KEY_PASSWORD` instead of `signing.properties`.
- GitHub Actions stores the encoded keystore and credentials in the dedicated `release` Environment. A `vX.Y.Z`
  tag must match `resonoteVersionName` in the root `gradle.properties` file.
- The release workflow creates a draft GitHub Release. Download and install the generated APK before publishing it.

Use `signing.properties.example` only as a field-name reference.
