package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle.ID
import org.jetbrains.annotations.PropertyKey

object UiToolsBundle : DynamicBundle(UiToolsBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.UiToolsBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
