package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.uuid

enum class UuidVersion(val title: String) {
  // -- Values -------------------------------------------------------------- //

  V1("UUIDv1"),
  V3("UUIDv3"),
  V4("UUIDv4"),
  V5("UUIDv5"),
  V6("UUIDv6"),
  V7("UUIDv7");

  override fun toString(): String = title

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
