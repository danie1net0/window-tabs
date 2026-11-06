# Window Tabs

IntelliJ Platform plugin (Kotlin) that shows open projects as tabs at the top of each IDE window. Targets every
JetBrains IDE 2024.3+ (Android Studio included); compiled and tested against IntelliJ IDEA 2024.3.6 (`platformVersion`
in `gradle.properties`), with the Kotlin API pinned to 2.0, the stdlib that 2024.3 bundles.

## Commands

- `./gradlew check`: ktlint, detekt, tests and the 100% line/branch coverage gate (Kover). Must pass before a commit.
- `./gradlew ktlintFormat`: fixes style.
- `./gradlew verifyPlugin`: plugin verifier against the oldest platform and the newest IDE of each family
  (`verifyIdes` in `gradle.properties`; downloads several GB the first time).
- `./gradlew buildPlugin`: zip in `build/distributions/`.

Gradle needs JDK 21+ (`JAVA_HOME`); detekt's JDK 21 toolchain is downloaded by the foojay resolver if missing.

## Rules

- Every class gets its own test file, mirroring its path under `src/test`.
- New or changed code keeps 100% line and branch coverage; no exclusions in the Kover config.
- Compiler warnings are errors; do not suppress detekt or ktlint findings without a reason in a comment.
- Platform tests extend `WindowTabsTestCase` (heavy test case with real projects). Window and focus calls that need
  real frames go through `fakeWindowManager()` / `fakeFrame()`, never through a real `JFrame`.
- Only platform API present in 2024.3 may be used; `verifyPlugin` against `IU-2024.3.6` catches anything newer.
- Windows runs the tests on GitHub Actions (`.github/workflows/windows.yml`), from the GitHub mirror.
- The tab strip uses the internal `ideRootPaneNorth` extension point; `verifyPlugin` only fails on real
  incompatibilities, so check its report for new internal API usages after upgrading the platform.

## Releases

1. Move the `[Unreleased]` notes in `CHANGELOG.md` under the new version and bump `pluginVersion` in `gradle.properties`.
2. Push a `vX.Y.Z` tag matching `pluginVersion`. The `sign`, `gitlab-release` and `marketplace` jobs sign the zip,
   attach it to a GitLab release and upload it to JetBrains Marketplace.
3. Signing reads the protected CI variables `CERTIFICATE_CHAIN_FILE`, `PRIVATE_KEY_FILE` (file type) and
   `PRIVATE_KEY_PASSWORD`; Marketplace publishing reads `PUBLISH_TOKEN`. Without them the plugin builds unsigned.
