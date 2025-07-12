package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManagerEx
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation

class TextDiffViewer(
  configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private val firstText = configuration.register("firstText", "", INPUT, FIRST_TEXT_EXAMPLE)
  private val secondText = configuration.register("secondText", "", INPUT, SECOND_TEXT_EXAMPLE)

  // -- Initialization ------------------------------------------------------ //

  init {
    wrapComponentInScrollPane = false
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        val firstDiffContent = createDiffContent(firstText)
        val secondDiffContent = createDiffContent(secondText)
        val diffComponent =
          DiffManagerEx.getInstance()
            .createRequestPanel(project, parentDisposable, null)
            .apply {
              setRequest(SimpleDiffRequest(null, firstDiffContent, secondDiffContent, null, null))
            }
            .component
        cell(diffComponent).resizableColumn().align(Align.FILL)
      }
      .resizableRow()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createDiffContent(textProperty: ValueProperty<String>): DiffContent {
    val document =
      EditorFactory.getInstance().createDocument(textProperty.get()).apply {
        addDocumentListener(
          object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
              textProperty.set(event.document.text, TEXT_CHANGE_FROM_DOCUMENT_LISTENER)
            }
          },
          parentDisposable,
        )
      }

    textProperty.afterChangeConsumeEvent(parentDisposable) { event ->
      if (event.id != TEXT_CHANGE_FROM_DOCUMENT_LISTENER) {
        runWriteAction { document.setText(event.newValue) }
      }
    }

    return DiffContentFactory.getInstance().create(project, document)
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextDiffViewer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "文本差异对比", contentTitle = "文本差异查看器")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextDiffViewer) = { configuration ->
      TextDiffViewer(
        configuration = configuration,
        project = project,
        parentDisposable = parentDisposable,
      )
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val TEXT_CHANGE_FROM_DOCUMENT_LISTENER = "documentChangeListener"

    private val FIRST_TEXT_EXAMPLE =
      """
      The sky is blue,
      The sun is shining,
      And the birds are singing.
      """
        .trimIndent()

    private val SECOND_TEXT_EXAMPLE =
      """
      The sky is gray,
      The rain is pouring,
      And the birds are silent.
      """
        .trimIndent()
  }
}
