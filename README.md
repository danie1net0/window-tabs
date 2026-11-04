<div align="center">

<img src="art/social-preview-1280x640.png" alt="Scattered project windows become tabs in a single window" width="640">

<br>

<p>
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.4">
  <img src="https://img.shields.io/badge/IntelliJ_Platform-2026.1+-000000?style=for-the-badge&logo=jetbrains&logoColor=white" alt="IntelliJ Platform 2026.1 or newer">
  <a href="https://gitlab.com/danie1net0/window-tabs/-/pipelines"><img src="https://img.shields.io/gitlab/pipeline-status/danie1net0%2Fwindow-tabs?branch=master&style=for-the-badge&label=CI" alt="Pipeline status"></a>
  <a href="https://gitlab.com/danie1net0/window-tabs/-/pipelines"><img src="https://img.shields.io/gitlab/pipeline-coverage/danie1net0%2Fwindow-tabs?branch=master&style=for-the-badge&label=coverage" alt="Test coverage measured in CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-F2C94C?style=for-the-badge" alt="MIT license"></a>
</p>

<sub><b>🇺🇸 English</b> · <a href="README.pt-BR.md">🇧🇷 Português</a></sub>

</div>

## Why

On macOS, JetBrains IDEs can merge project windows into native window tabs. Linux has no such feature, so every
project opens in its own window and switching means hunting through Alt+Tab. Window Tabs adds a tab strip to the top
of every PhpStorm and GoLand window (any JetBrains IDE 2026.1+ works) with one tab per open project.

- **Click a tab**: that project's window takes the current window's size and position (or stays maximized when the
  current one is) and comes to the front.
- **Middle-click or `×`** (shown on hover): closes the project.
- **`+`** on the right: opens another project.
- The strip shows up only while two or more projects are open.

Each project is still its own window; the plugin makes switching between them feel like switching tabs.

## Install

Download `window-tabs-<version>.zip` from the artifacts of the latest
[`build` job](https://gitlab.com/danie1net0/window-tabs/-/pipelines), or build it yourself (below). Then, in the IDE:
**Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the zip. No restart needed.

## Linux / GNOME

- On Wayland, GNOME may refuse to raise the window and show a "PhpStorm is ready" notice instead. If that happens, run
  the IDE on X11: **Help → Edit Custom VM Options** → `-Dawt.toolkit.name=XToolkit`.
- Keep the windows maximized: switching is instant and windows do not jump around.
- GNOME's Alt+Tab still lists one window per project. Super+` (*Switch windows of an application*) cycles through
  them too.

## Build

Gradle downloads IntelliJ IDEA 2026.1.5, the oldest supported platform, and compiles and tests against it. Any JDK 21+
runs the build; on a machine without one, point `JAVA_HOME` at the IDE's bundled JBR:

```bash
# macOS
export JAVA_HOME=/Applications/PhpStorm.app/Contents/jbr/Contents/Home
# Linux (Toolbox)
export JAVA_HOME=~/.local/share/JetBrains/Toolbox/apps/phpstorm/jbr
```

```bash
./gradlew buildPlugin
```

The zip lands in `build/distributions/`.

## Quality

```bash
./gradlew check
```

Runs everything the CI requires:

- **ktlint**: the official Kotlin style (`./gradlew ktlintFormat` fixes it).
- **detekt**: static analysis, default rules plus `config/detekt.yml`.
- **Compiler**: every warning is an error.
- **Tests**: unit tests and platform tests (a headless IDE opening and closing real projects).
- **Kover**: 100% of the plugin's lines and branches must be covered, or the build fails.

Compatibility with PhpStorm and GoLand (`verifyIdeVersion` in `gradle.properties`; downloads both IDEs):

```bash
./gradlew verifyPlugin
```

The GitLab pipeline runs `ktlint` and `detekt`, then `test` and `verify`, and finally `build`, which keeps the zip as
an artifact.

## Error reports

When the plugin throws an exception or freezes the IDE, the IDE Internal Errors dialog offers
**Report to third-party plugin**. The report goes to JetBrains Marketplace, which shows it in the plugin's Exceptions
and Freezes tabs, with the stack trace, OS, IDE version and plugin version. Nothing is sent without the user's consent,
except for users who turned on **Send error reports to JetBrains automatically** in the IDE.

## Limitation

The strip uses the `ideRootPaneNorth` extension point, which JetBrains marks as internal. `verifyPlugin` accepts it
(it only fails on real incompatibilities), but a future IDE release may change that API.

## Repository

The code lives at [gitlab.com/danie1net0/window-tabs](https://gitlab.com/danie1net0/window-tabs).
[github.com/danie1net0/window-tabs](https://github.com/danie1net0/window-tabs) is a read-only mirror.

## License

[MIT](LICENSE)
