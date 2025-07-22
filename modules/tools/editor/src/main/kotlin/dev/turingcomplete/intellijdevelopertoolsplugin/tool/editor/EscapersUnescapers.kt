package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import org.apache.commons.text.StringEscapeUtils

object EscapersUnescapers {
  // -- Variables ----------------------------------------------------------- //

  private val log = logger<EscapersUnescapers>()

  val commonEscaper =
    listOf(
      Escaper(UiToolsBundle.message("escape.unescape.group.escape.java-string.title"), { StringEscapeUtils.escapeJava(it) }),
      Escaper(UiToolsBundle.message("escape.unescape.group.escape.html.entities.title") , { StringEscapeUtils.escapeHtml4(it) }),
      Escaper(UiToolsBundle.message("escape.unescape.group.escape.json-value.title"), { StringEscapeUtils.escapeJson(it) }),
      Escaper(UiToolsBundle.message("escape.unescape.group.escape.xml-value.title"), { StringEscapeUtils.escapeXml11(it) }),
      Escaper(UiToolsBundle.message("escape.unescape.group.escape.csv-value.title") , { StringEscapeUtils.escapeCsv(it) }),
    )

  val commonUnescaper =
    listOf(
      Unescaper(UiToolsBundle.message("escape.unescape.group.escape.java-string.title"), { StringEscapeUtils.unescapeJava(it) }),
      Unescaper(UiToolsBundle.message("escape.unescape.group.escape.html.entities.title"), { StringEscapeUtils.escapeHtml4(it) }),
      Unescaper(UiToolsBundle.message("escape.unescape.group.escape.json-value.title"), { StringEscapeUtils.unescapeJson(it) }),
      Unescaper(UiToolsBundle.message("escape.unescape.group.escape.xml-value.title"), { StringEscapeUtils.unescapeXml(it) }),
      Unescaper(UiToolsBundle.message("escape.unescape.group.escape.csv-value.title"), { StringEscapeUtils.unescapeCsv(it) }),
    )

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun executeEscapeInEditor(text: String, textRange: TextRange, escaper: Escaper, editor: Editor) {
    try {
      val result = escaper.escape(text)
      editor.executeWriteCommand(escaper.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    } catch (e: Exception) {
      log.warn(UiToolsBundle.message("escape.unescape.group.escape.failed.title"), e)
      ApplicationManager.getApplication().invokeLater {
        e.message?.let { Messages.showErrorDialog(editor.project, UiToolsBundle.message("escape.unescape.group.escape.failed.message.title","${e.message}") , escaper.actionName) }
      }
    }
  }

  fun executeUnescapeInEditor(
    text: String,
    textRange: TextRange,
    unescaper: Unescaper,
    editor: Editor,
  ) {
    try {
      val result = unescaper.unescape(text)
      editor.executeWriteCommand(unescaper.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    } catch (e: Exception) {
      log.warn(UiToolsBundle.message("escape.unescape.group.unescape.failed.title"), e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(
          editor.project,
          UiToolsBundle.message("escape.unescape.group.unescape.failed.message.title","${e.message}"),
          unescaper.actionName,
        )
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Escaper(
    val title: String,
    val escape: (String) -> String,
    val actionName: String = UiToolsBundle.message("escape.unescape.group.escape.action.title",title),
  )

  // -- Inner Type ---------------------------------------------------------- //

  class Unescaper(
    val title: String,
    val unescape: (String) -> String,
    val actionName: String = UiToolsBundle.message("escape.unescape.group.unescape.action.title",title),
  )
}
