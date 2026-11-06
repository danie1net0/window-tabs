# Changelog

All notable changes to Window Tabs are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com),
and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- Alt+Shift+1…9 switch to the project in that tab; Next Project Tab and Previous Project Tab (Window | Project Tabs) walk the
  strip and can get a shortcut in the keymap.
- Tabs can be dragged into a new order, and the order is kept for the next start.
- When the tabs do not fit, they keep a readable width and the strip scrolls with the mouse wheel, always showing the
  current project's tab.
- Each tab shows the project's icon, the same one the Welcome screen uses.
- Settings | Appearance & Behavior | Window Tabs: show the strip with a single project, hide the project icons, show
  the strip on macOS.

### Changed

- Works in every JetBrains IDE from 2024.3 on, Android Studio included (was 2026.1).
- The strip stays off on macOS by default, where the IDE can already merge project windows into native tabs.
- The dimmed tabs of the last session go away once no project has opened for 30 seconds, since older IDEs do not
  report the end of the reopen.

## [0.1.0] - 2026-11-04

### Added

- A tab strip at the top of every IDE window with one tab per open project.
- Clicking a tab moves that project's window over the current one and brings it to the front.
- Middle-click or the close icon closes the project; the plus button opens another one.
- While the IDE reopens the last session, the projects still on their way show as dimmed tabs.
- Exceptions and UI freezes attributed to the plugin can be reported to JetBrains Marketplace.

[Unreleased]: https://gitlab.com/danie1net0/window-tabs/-/compare/v0.1.0...master
[0.1.0]: https://gitlab.com/danie1net0/window-tabs/-/tags/v0.1.0
