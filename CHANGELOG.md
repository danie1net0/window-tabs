# Changelog

All notable changes to Window Tabs are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com),
and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

### Fixed

- The saved tab order survives quitting the IDE, which closes its projects one by one.
- Closing the last project from its tab brings the Welcome screen back instead of leaving the IDE with no window.
- Switching to a project from a maximized window maximizes it on the same monitor.
- A short click that moves the mouse a few pixels still switches tabs; only the left button drags.
- The strip keeps where it was scrolled to when it refreshes, and waits for a drag to end before refreshing.
- No dimmed tabs for the last session when the IDE starts with another project instead of reopening it.
- The badge colour no longer depends on whether the IDE had already drawn the project icon, is worked out once per
  project, and a broken project icon no longer empties the strip. A project's own icon shows as drawn, at the tab's size.
- A drag whose release never arrives, as when a dialog opens mid-drag, is called off by the next press instead of
  freezing the strip.
- Switching between two maximized windows on the same monitor keeps the target's restore size.
- The close and plus buttons react to the left button only, and to a click that moves a few pixels.
- Projects kept in an .ipr file match their last-session entry, so they no longer show twice while the IDE reopens.
- The saved tab order keeps at most 100 projects, forgetting the ones closed longest ago first.
- A drop lands in front of the tab the user saw, even if a tab opened or closed during the drag.
- Pressing another mouse button mid-drag neither drops the tab nor clicks.
- Detached editor windows no longer get a strip of their own.
- A tab's title gets its full width back when the mouse leaves it.
- Refreshing the strip no longer checks the disk for project icons each time.

## [0.2.0] - 2026-11-19

### Added

- Alt+Shift+1…9 switch to the project in that tab; Next Project Tab and Previous Project Tab (Window | Project Tabs) walk the
  strip and can get a shortcut in the keymap.
- Tabs can be dragged into a new order: the dragged tab follows the mouse while the others slide aside, and the order
  is kept for the next start.
- When the tabs do not fit, they keep a readable width and the strip scrolls with the mouse wheel, always showing the
  current project's tab.
- Each tab shows a rounded badge with the project's initials, in the colour of its Welcome screen icon.
- Settings | Appearance & Behavior | Window Tabs: show the strip with a single project, hide the project icons, show
  the strip on macOS.

### Changed

- Works in every JetBrains IDE from 2024.3 on, Android Studio included (was 2026.1).
- The tabs look like macOS window tabs: the selected one merges with the toolbar, the others sit recessed behind it
  under a soft shadow, and the titles stay centered when the close button appears.
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

[Unreleased]: https://gitlab.com/danie1net0/window-tabs/-/compare/v0.2.0...master
[0.2.0]: https://gitlab.com/danie1net0/window-tabs/-/compare/v0.1.0...v0.2.0
[0.1.0]: https://gitlab.com/danie1net0/window-tabs/-/tags/v0.1.0
