package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling

interface OpenDeveloperToolHandler<T : OpenDeveloperToolContext> {
  // -- Properties ---------------------------------------------------------- //
  // -- Exported Methods ---------------------------------------------------- //

  fun applyOpenDeveloperToolContext(context: T)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
