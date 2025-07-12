package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.AUTOMATIC_DETECTION
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.FIXED_TEXT_CASE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.INDIVIDUAL_DELIMITER
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.TextCase.COBOL_CASE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.TextCase.STRICT_CAMEL_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.TextCase as StandardTextCase
import dev.turingcomplete.textcaseconverter.toTextCase
import dev.turingcomplete.textcaseconverter.toWordsSplitter

class TextCaseTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?,
) :
  UndirectionalConverter(
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
    title = "文本大小写",
    sourceTitle = "原文本",
    targetTitle = "目标文本",
    toTargetTitle = "转换",
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var originalParsingMode =
    configuration.register("originalParsingMode", AUTOMATIC_DETECTION)
  private var individualDelimiter = configuration.register("individualDelimiter", " ")
  private var inputTextCase = configuration.register("inputTextCase", STRICT_CAMEL_CASE)
  private var outputTextCase = configuration.register("outputTextCase", COBOL_CASE)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_SOURCE_TEXT,
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val sourceText = String(source)
    val wordsSplitter = getInputWordsSplitter(sourceText)
    return sourceText.toTextCase(outputTextCase.get().textCase, wordsSplitter).toByteArray()
  }

  override fun Panel.buildSourceBottomConfigurationUi() {
    buttonsGroup("原文本:") {
      row { radioButton("自动检测").bind(originalParsingMode, AUTOMATIC_DETECTION) }

      row {
        val fixedTextCaseRadioButton =
          radioButton("固定文本大小写:").bind(originalParsingMode, FIXED_TEXT_CASE).gap(RightGap.SMALL)
        comboBox(TextCase.entries)
          .bindItem(inputTextCase)
          .enabledIf(fixedTextCaseRadioButton.selected)
          .component
      }

      row {
        val individualDelimiterRadioButton =
          radioButton("单词拆分依据:").bind(originalParsingMode, INDIVIDUAL_DELIMITER).gap(RightGap.SMALL)
        textField()
          .bindText(individualDelimiter)
          .enabledIf(individualDelimiterRadioButton.selected)
          .component
      }
    }

    row { comboBox(TextCase.entries).label("目标:").bindItem(outputTextCase) }
  }

  //  override fun Panel.buildDebugComponent() {
  //    group("Input Words") {
  //      row {
  //        val inputWords = getInputWordsSplitter().split(sourceText.get())
  //        if (inputWords.isEmpty()) {
  //          label("<html><i>None</i></html>")
  //        } else {
  //          val wordsList =
  //            inputWords.joinToString(separator = "", prefix = "<ol>", postfix = "</ol>") {
  //              "<li>$it</li>"
  //            }
  //          label("<html>$wordsList</html>")
  //        }
  //      }
  //    }
  //  }

  // -- Private Methods ----------------------------------------------------- //

  private fun getInputWordsSplitter(sourceText: String) =
    when (originalParsingMode.get()) {
      AUTOMATIC_DETECTION ->
        TextCaseUtils.determineWordsSplitter(sourceText, inputTextCase.get().textCase)
      FIXED_TEXT_CASE -> inputTextCase.get().textCase.wordsSplitter()
      INDIVIDUAL_DELIMITER -> individualDelimiter.get().toWordsSplitter()
    }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class TextCase(val textCase: StandardTextCase) {

    SCREAMING_SNAKE_CASE(StandardTextCases.SCREAMING_SNAKE_CASE),
    SOFT_CAMEL_CASE(StandardTextCases.SOFT_CAMEL_CASE),
    STRICT_CAMEL_CASE(StandardTextCases.STRICT_CAMEL_CASE),
    PASCAL_CASE(StandardTextCases.PASCAL_CASE),
    SNAKE_CASE(StandardTextCases.SNAKE_CASE),
    KEBAB_CASE(StandardTextCases.KEBAB_CASE),
    TRAIN_CASE(StandardTextCases.TRAIN_CASE),
    COBOL_CASE(StandardTextCases.COBOL_CASE),
    PASCAL_SNAKE_CASE(StandardTextCases.PASCAL_SNAKE_CASE),
    CAMEL_SNAKE_CASE(StandardTextCases.CAMEL_SNAKE_CASE),
    LOWER_CASE(StandardTextCases.LOWER_CASE),
    UPPER_CASE(StandardTextCases.UPPER_CASE),
    INVERTED_CASE(StandardTextCases.INVERTED_CASE),
    ALTERNATING_CASE(StandardTextCases.ALTERNATING_CASE),
    DOT_CASE(StandardTextCases.DOT_CASE);

    //    override fun toString(): String = "${textCase.title()} (${textCase.example()})"

    override fun toString(): String =
      when (this) {
        SCREAMING_SNAKE_CASE -> "全大写蛇形命名 (SCREAMING_SNAKE_CASE)"
        SOFT_CAMEL_CASE -> "软驼峰命名 (softCamelCase)"
        STRICT_CAMEL_CASE -> "严格驼峰命名 (strictCamelCase)"
        PASCAL_CASE -> "帕斯卡命名 (PascalCase)"
        SNAKE_CASE -> "蛇形命名 (snake_case)"
        KEBAB_CASE -> "烤肉串命名 (kebab-case)"
        TRAIN_CASE -> "火车命名 (TRAIN-CASE)"
        COBOL_CASE -> "COBOL 命名 (COBOL-CASE)"
        PASCAL_SNAKE_CASE -> "帕斯卡蛇形命名 (Pascal_Snake_Case)"
        CAMEL_SNAKE_CASE -> "驼峰蛇形命名 (camel_Snake_Case)"
        LOWER_CASE -> "全小写 (lowercase)"
        UPPER_CASE -> "全大写 (UPPERCASE)"
        INVERTED_CASE -> "大小写反转 (InVeRtEdCaSe)"
        ALTERNATING_CASE -> "大小写交替 (aLtErNaTiNgCaSe)"
        DOT_CASE -> "点分隔命名 (dot.case)"
        else -> textCase.title()
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class OriginalParsingMode {

    AUTOMATIC_DETECTION,
    FIXED_TEXT_CASE,
    INDIVIDUAL_DELIMITER,
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextCaseTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "文本大小写", contentTitle = "文本大小写转换")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextCaseTransformer) = { configuration ->
      TextCaseTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    const val EXAMPLE_SOURCE_TEXT = "thisIsAnExampleText"
  }
}
