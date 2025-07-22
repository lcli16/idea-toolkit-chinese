# Changelog

## Unreleased

### Added

### Changed

### Removed

### Fixed

## 7.2.1 - 2025-07-22

### Added

### Changed
- 修复兼容性问题
- 完善剩余部分功能翻译
- 优化页面

## 7.2.0 - 2025-07-22

### Added
- 新增AES加密解密功能

### Changed


## 7.1.2 - 2025-07-12

### Added


### Changed

-  修改一下插件名称

## 7.1.1 - 2025-07-12

### Added

- 修复兼容性 问题

### Changed

-  修复兼容性 问题

## 7.1.0 - 2025-05-18

### Added

- New "Cron Expression" tool

### Changed

- By default, the UI of the tools in the tool window will no longer be cached and will instead be rebuilt on demand to reduce memory usage. This behavior can be changed in the settings to prioritize responsiveness.

## 7.0.0 - 2025-04-27

### Added

- All encoding/decoding, escaping/unescaping and text transformation tools now also support reading from and writing to files.
- New "Escape Sequence" tool for escaping/unescaping line breaks, tabs, backslashes, and single/double quotes.
- New "JSON Handling" settings that allow very fine-grained control over the features for reading and writing JSON in all tools. This makes it possible to handle certain non-standard JSON features, such as comments.
- Added a setting to control the number of decimal places in the "Color Picker" tool.

### Changed

- Setting "Dialog is modal" was reset to its default value (false), due to overhaul of the internal settings handling.

### Removed

- The "Line Breaks Encoding" tool has been replaced by the new "Escape Sequence" tool.

### Fixed

- Fixed compatibility problems with 2025.1 releases.
- The `hsl/hsla` CSS color value wasn't correctly calculated in the "Color Picker" tool.
- JSON input errors contained too much irrelevant metadata about the internal JSON processing.

## 6.3.0 - 2025-01-14

### Added

- Tool "Regular Expression" now supports substitution and extraction

### Fixed

- Tool "ASCII Art" was not accessible with non-IntelliJ IDEs

## 6.2.2 - 2025-01-08

### Fixed

- Improve compatibility with non-IntelliJ IDEs

## 6.2.1 - 2025-01-05

### Fixed

- The "ASCII Art" tool was incompatible with non-IntelliJ IDEs

## 6.2.0 - 2025-01-03

### Added

- Add tool "Server Certificates" to fetch, analyse and export server certificates
- Add tool "ASCII Art"

### Changed

- Tool "Date Time" renamed to "Date and Time"

## 6.1.0 - 2024-12-14

### Added

- Add ASCII Encoder/Decoder tool

### Changed

- Improve title of tools in the menu

### Fixed

- Copy action in the Password Generator tool contained escaped HTML entities

## 6.0.1 - 2024-11-22

### Fixed

- Fix the validation and generation of the signature part of JWT encoder/decoder.

## 6.0.0 - 2024-11-14

### Added

- Add number base converter to the tool _Units Converter_.
- Compatibility with IntelliJ 2024.3

## 5.1.0 - 2024-09-20

### Added

- New tool _Units Converter_ that supports data sizes and transfer rates conversion.
- Add characters count to the _Text Statistic_ tool.
- Add support for Kotlin K2 compiler.

### Changed

- Tool "Time Conversion" moved into the new "Units Converter" tool.
- Setting _save secret inputs_ renamed to _save sensitive inputs_.
- The minimum required IntelliJ version is now 2024.1. Drop of support for the major release version 4.

### Removed

- Remove support to store secret inputs in the system password store.

### Fixed

- The _Text Diff_ tool UI will no longer add an unnecessary scrollbar if the text is larger than the visible editor.
- Errors in the _Date Time_ tool only block the conversion if this error occurs in the last active input field.

## 5.0.0 - 2024-05-21

### Added

- Compatibility with IntelliJ 2024.2 EAP
- The _Text Diff_ tool UI will no longer add an unnecessary scrollbar if the text is larger than the visible editor.

## 4.3.0 - 2024-05-20

### Added

- Add keymap actions to show a developer tool
- Add optional strict secret/key requirements check to the _JSON Web Token (JWT) Decoder/Encoder_ tool.
- Add gutter icon with readable a readable date/time format for UNIX timestamps in the _JSON Web Token (JWT) Decoder/Encoder_ tool.
- The "JSON Web Token (JWT) Decoder/Encoder" tool interface now includes sliders to flexibly change the size of the editors.
- Add new tool _Text Filter_.

### Changed

- The tool "JWT Decoder/Encoder" renamed to "JSON Web Token (JWT) Decoder/Encoder".
- Context menu action "Text Statistic..." was renamed to "Show Text Statistic of Document...".

### Removed

- The input of a public key for the JWT signature configuration was removed from the "JSON Web Token (JWT) Decoder/Encoder" tool.

## 4.2.0 - 2024-04-08

### Changed

- Lower IntelliJ compatibility to 2023.2 to support the latest Android Studio.
- In the tool window, the tools menu is now available through a separated action button.
- The workbench tabs are now hidden by default when there is only one tab. This behaviour can be changed in the settings. Creating a new workbench is now also available from the tools actions popup.

## 4.1.1 - 2024-03-28

### Fixed

- Fix incompatibility problems with IntelliJ 2024.1

## 4.1.0 - 2024-03-24

### Added

- Add automatic input text case detection to the text case converter
- Add escape/unescape as editor actions and code intentions
- Add new tool: Text Statistic
- Add support for the "Dot Text Case"
- Add common hashing algorithms to the encoding editor action and code intention
- Add common SHA3 algorithms to the random data generator editor action

### Fixed

- Editor actions on Java Strings will now preserve the outer String quotations

## 4.0.0 - 2024-03-10

### Added

- Some tools (data generators, encoders/decoders and text case conversion) are now also available in the Editor menu or code intentions. Some of these actions are only available if a text is selected, or the current caret position is on a Java/Kotlin string or identifier.
- Extend the ULID generator for monotonic ULIDs
- New tool "IntelliJ Internals"

## 3.5.0 - 2024-02-28

### Added

- New tool 'Unarchiver' to analyze and extract archive files

### Changed

- Improve UI of the Regular Expression Matcher tool

### Fixed

- Lorem Ipsum text was regenerated each time the tool window was opened

## 3.4.0 - 2024-01-21

### Added

- Settings option to enable or disable the grouping of tools
- Settings option to sort the tools menu alphabetically

### Changed

- By default, the tools menu is a flat alphabetical list. The old behavior (e.g., grouping of nodes) can be restored through the settings.
- Removed the setting to hide the tool window menu on a tool selection. The selection mechanism now distinguishes between an automatic search result selection (the menu remains visible) and an user selection (a menu is hidden).
- Renamed tool "Java Text Escape/Unescape" to "Java String Escape/Unescape"
- Renamed tool "Code Formatting Converter" to "Text Format Converter"
- Text related tools moved to new group "text"

### Fixed

- Tool "Code Format Converter" wasn't working correctly

## 3.3.0 - 2024-01-15

### Added

- Add time conversion tool
- Add 'What's New' overview to the main menu

### Fixed

- Inputs and configurations for tools that were not opened were lost the next time the settings were saved.
- Loading the tool window settings causes an error if one of the tools has a saved secret.
- In the JWT Encoder/Decoder the "Public Key" field was visible for the HMAC signature algorithm.

## 3.2.0 - 2024-01-06

### Added

- Add ULID generator tool
- Add Nano ID generator tool
- Add color picker tool
- Add an action to open the Developer Tools section in IntelliJ's settings
- Improve the layout of the tools in the tool window

### Changed

- The UUID generator was moved to the "Cryptography" group

## 3.1.0 - 2023-12-29

### Added

- A new option in the settings to add the 'Developer Tools' action to the main toolbar during startup

### Changed

- The tool window is not activated on startup anymore if it was previously open, to avoid negatively impacting IntelliJ's startup time.

### Fixed

- Opening IntelliJ's settings window will reset the configuration to the default values.

## 3.0.0 - 2023-12-28

### Added

- The tools are now available through a tool window.

### Changed

- The Open Dialog action is still available but no longer automatically added to the main menu to favor the new tool window. To restore the old behavior, the action can be added again via **Customize Toolbar... | Add Actions... | Developer Tools**.
- Settings have been moved to IntelliJ's settings window

### Fixed

- Configuration reset does not reset default editor settings
- Setting "Remember configurations" wasn't persisted

## 2.0.1 - 2023-10-30

### Removed

- Remove not fully implemented JSON path library switch

## 2.0.0 - 2023-10-28

### Added

- Add CLI Command line breaks converter
- Add a "Set to Now" button to the date time converter
- Add capability to remember editor settings
- Add "Show Special Characters" setting to the editor
- Add "Show Whitespaces" setting to the editor
- Add default editor settings to the configuration
- Add an icon to indicate the current live conversion in text converters
- Add Base32/Base64 encoding capability for HMAC and JWT secrets ([GitHub Issue #16](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/16))
- Add automatic formatting option for the JSON patch result ([GitHub Issue #15](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/15))
- Add expand option to some text fields

### Fixed

- Fix date time converter ignores selected time zone ([GitHub Issue #11](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/11))

## 1.1.0 - 2023-08-14

### Added

- Add "Expand Editor" action to editors
- Add more details of a date in the date time converter
- Add Base64 secret key handling for the HMAC transformer ([GitHub Issue 5](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/5))

### Fixed

- Fix wrong naming of encoders/decodes input/output text areas ([GitHub Issue #4](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/4))
- Fix invalid date time format prevents usage of a standard format in the date time converter
- Fix individual date time format is not restored in the date time converter ([GitHub Issue #8](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/8))

### Changed

- Improve editor sizes in the JWT Encoder/Decoder
- Remove dependency on code from the JsonPath plugin ([GitHub Issue #9](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/9))

## 1.0.1 - 2023-05-29

### Fixed

- IntelliJ SDK compatibility

## 1.0.0 - 2023-05-29

### Added

- Initial release
