# IntelliJ Gradle Updater

## [Unreleased]

## [2.3.0]
- No longer use internal IntelliJ API (Fix #23)
- Upgrade Kotlin to 1.7.10
- Upgrade ktor to 2.1.1
- Upgrade IntelliJ to 2022.2
- Upgrade JDK to 17
- Upgrade kotlinx.serialization to 1.4.0
- Upgrade kotlinx.coroutines to 1.6.4
- Upgrade Sentry to 6.4.1

## [2.2.0]
### Changes
- Upgrade to Kotlin 1.6.20
- Upgrade to Ktor 2.0.0
- Upgrade to IntelliJ 2022.1

### Fixed
- Fix issue of Plugin detecting dependency formats false positively (maven("https://abc.de") -> maven("http", "//abc.de")

## [2.1.0]
### Changed
- Update to IntelliJ 2021.3

### Fixed
- Gradle update notification appearing on latest Gradle version
- Kotlin format converter triggering when converting Gradle snippets

## [2.0.4]
### Changed
- Update to IntelliJ 2021.2

## [2.0.3]
- Fix compatibility range to include all 2021.1 versions
- Fix compatibility problems

## [2.0.2]
### Added
- Add CI

### Changed
- Bump dependencies

### Fixed
- Fix bug when trying to load settings file