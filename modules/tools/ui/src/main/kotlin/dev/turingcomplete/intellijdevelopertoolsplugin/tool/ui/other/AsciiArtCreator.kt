package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.github.lalyos.jfiglet.FigletFont
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.common.GitHubUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.clearDirectory
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor.Companion.defaultUiInputDelay
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.hyperLink
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton

class AsciiArtCreator(
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable,
  private val context: DeveloperUiToolContext,
) : DeveloperUiTool(parentDisposable = parentDisposable), DataProvider {
  // -- Properties ---------------------------------------------------------- //

  private val log = logger<AsciiArtCreator>()

  private val textInput = configuration.register("textInput", "", INPUT, "Awesome")
  private val selectedFontFileName =
    configuration.register("selectedFontFileName", DEFAULT_BUILT_IN_FILE_NAME, CONFIGURATION)

  private val asciiArtOutputErrorHolder = ErrorHolder()
  private val asciiArtOutput = ValueProperty("")

  private lateinit var fontFileNamesComboBoxModel: FontFileNamesComboBoxModel
  private val fontResources: MutableMap<String, () -> InputStream> = mutableMapOf()

  private val syncFontsAlarm by lazy { AsyncTaskExecutor.onEdt(parentDisposable) }
  private val createAsciiArtAlarm by lazy { AsyncTaskExecutor.onEdt(parentDisposable) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    row {
      expandableTextField()
        .bindText(textInput)
        .resizableColumn()
        .whenTextChangedFromUi { createAsciiArt() }
        .resizableColumn()
        .align(Align.FILL)
    }

    row {
      fontFileNamesComboBoxModel = FontFileNamesComboBoxModel()
      comboBox(fontFileNamesComboBoxModel)
        .label(UiToolsBundle.message("ascii-art.font"))
        .bindItem(selectedFontFileName)
        .whenItemSelectedFromUi { createAsciiArt() }
        .gap(RightGap.SMALL)
        .applyToComponent { prototypeDisplayValue = "x".repeat(30) }
      hyperLink(
        UiToolsBundle.message("ascii-art.examples"),
        "https://github.com/xero/figlet-fonts/blob/master/Examples.md",
      )
    }

    row {
        val editor =
          AdvancedEditor(
              id = "asciiArtOutput",
              context = context,
              configuration = configuration,
              project = project,
              title = UiToolsBundle.message("ascii-art.output-title"),
              editorMode = AdvancedEditor.EditorMode.OUTPUT,
              parentDisposable = parentDisposable,
              textProperty = asciiArtOutput,
              fixedEditorSoftWraps = false,
            )
            .onTextChangeFromUi { createAsciiArt() }
        cell(editor.component)
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .validationOnApply(editor.bindValidator(asciiArtOutputErrorHolder.asValidation()))
          .resizableColumn()
          .align(Align.FILL)
      }
      .resizableRow()
      .topGap(TopGap.MEDIUM)
      .bottomGap(BottomGap.MEDIUM)

    row {
      lateinit var downloadFontsButton: JButton
      downloadFontsButton =
        button(UiToolsBundle.message("ascii-art.download-additional-ascii-art-fonts")) {
            downloadAdditionalAsciiArtFonts(downloadFontsButton)
          }
          .gap(RightGap.SMALL)
          .component
      contextHelp(UiToolsBundle.message("ascii-art.download-additional-ascii-art-fonts-help"))
    }
  }

  override fun afterBuildUi() {
    syncFonts()
  }

  override fun reset() {
    createAsciiArt()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun downloadAdditionalAsciiArtFonts(downloadFontsButton: JButton) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        downloadedFontsPath.clearDirectory()
      } catch (e: Exception) {
        log.warn("Failed to clear ASCII art fonts download directory: $downloadedFontsPath", e)
      }
      ApplicationManager.getApplication().invokeLater { syncFonts() }

      GitHubUtils.downloadFiles(
          project = project!!,
          repositoryUrl = "https://github.com/xero/figlet-fonts",
          destinationPath = downloadedFontsPath,
          preDownloadFilter = { fileName -> fileName.endsWith(".flf") },
          afterDownloadFilter = { file ->
            try {
              createAsciiArt(Files.newInputStream(file), UiToolsBundle.message("ascii-art.example"))
              true
            } catch (e: Exception) {
              log.warn(
                "Failed to render example ASCII art using font file: $file. Font file will be ignored.",
                e,
              )
              false
            }
          },
          onStart = { downloadFontsButton.isEnabled = false },
          onSuccess = {},
          onThrowable = {
            ApplicationManager.getApplication().invokeLater {
              Messages.showErrorDialog(
                project,
                UiToolsBundle.message(
                  "ascii-art.download-additional-ascii-art-fonts-failed-details"
                ),
                UiToolsBundle.message("ascii-art.download-additional-ascii-art-fonts-failed-title"),
              )
            }
          },
          onFinished = {
            syncFonts()
            downloadFontsButton.isEnabled = true
          },
        )
        .queue()
    }
  }

  private fun createAsciiArt() {
    if (isDisposed) {
      return
    }
    createAsciiArtAlarm.replaceTasks(defaultUiInputDelay) {
      asciiArtOutputErrorHolder.clear()

      try {
        var fontResource = fontResources[selectedFontFileName.get()]
        if (fontResource == null) {
          selectedFontFileName.set(DEFAULT_BUILT_IN_FILE_NAME)
          fontResource = fontResources[DEFAULT_BUILT_IN_FILE_NAME]
        }

        asciiArtOutput.set(createAsciiArt(fontResource!!.invoke(), textInput.get()))
      } catch (e: Exception) {
        asciiArtOutputErrorHolder.add(e)
      }
    }
  }

  private fun createAsciiArt(fontResource: InputStream, text: String): String =
    fontResource.use {
      FigletFont.convertOneLine(it, text)
        .lines()
        .dropWhile { it.isBlank() }
        .dropLastWhile { it.isBlank() }
        .joinToString(System.lineSeparator())
    }

  private fun syncFonts() {
    if (isDisposed || syncFontsAlarm.isDisposed) {
      return
    }
    syncFontsAlarm.replaceTasks {
      val fontResources = mutableMapOf<String, () -> InputStream>()

      builtInFonts.forEach {
        val fontResource = getBuiltInFontResource(it)
        if (fontResource != null) {
          fontResources.put(it) { getBuiltInFontResource(it)!! }
        } else {
          log.warn("Built-in font $it not found")
        }
      }

      if (Files.exists(downloadedFontsPath)) {
        Files.list(downloadedFontsPath)
          .filter { Files.isRegularFile(it) }
          .forEach { fontResources.put(it.fileName.toString()) { Files.newInputStream(it) } }
      }

      this.fontResources.clear()
      this.fontResources.putAll(fontResources)

      if (!fontResources.containsKey(selectedFontFileName.get())) {
        selectedFontFileName.set(DEFAULT_BUILT_IN_FILE_NAME)
      }

      fontFileNamesComboBoxModel.setFileNames(fontResources.keys, selectedFontFileName.get())

      createAsciiArt()
    }
  }

  private fun getBuiltInFontResource(fontFileName: String): InputStream? =
    FigletFont::class.java.getClassLoader().getResourceAsStream(fontFileName)

  // -- Inner Type ---------------------------------------------------------- //

  private class FontFileNamesComboBoxModel() : DefaultComboBoxModel<String>() {

    private val fontFileNames: MutableList<String> = mutableListOf()

    override fun getSize(): Int = fontFileNames.size

    override fun getElementAt(index: Int): String? = fontFileNames[index]

    fun setFileNames(fileNames: Collection<String>, selectedFontFileName: String) {
      fontFileNames.clear()
      fontFileNames.addAll(fileNames.sorted())

      if (fileNames.isNotEmpty()) {
        // This will also call `fireContentsChanged`
        selectedItem = selectedFontFileName
      } else {
        fireContentsChanged(this, -1, -1)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<AsciiArtCreator> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("ascii-art.menu-title"),
        contentTitle = UiToolsBundle.message("ascii-art.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> AsciiArtCreator) = { configuration ->
      AsciiArtCreator(
        configuration = configuration,
        project = project,
        parentDisposable = parentDisposable,
        context = context,
      )
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    const val DEFAULT_BUILT_IN_FILE_NAME = "standard.flf"
    private val builtInFonts = listOf<String>(DEFAULT_BUILT_IN_FILE_NAME, "slant.flf")
    private val downloadedFontsPath =
      PathManager.getSystemDir().resolve(Paths.get("plugins", "developer-tools", "ascii-fonts"))
  }
}
