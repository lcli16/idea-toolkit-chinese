package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.emptyByteArray
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex.RegexTextField
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex.SelectRegexOptionsAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter

class TextFilterTransformer(
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
    title = "文本筛选",
    sourceTitle = "未过滤",
    targetTitle = "筛选",
    toTargetTitle = "过滤器",
  ) {
  // -- Properties ---------------------------------------------------------- //

  private val tokenMode = configuration.register("tokenSelectionMode", DEFAULT_TOKEN_SELECTION_MODE)
  private val filteringMode = configuration.register("filteringMode", DEFAULT_FILTERING_MODE)
  private val filteringContainingModeText =
    configuration.register(
      "filteringContainingModeText",
      "",
      INPUT,
      EXAMPLE_FILTERING_CONTAINING_MODE_TEXT,
    )
  private val filteringNotContainingModeText =
    configuration.register(
      "filteringNotContainingModeText",
      "",
      INPUT,
      EXAMPLE_FILTERING_NOT_CONTAINING_MODE_TEXT,
    )
  private val filteringRegexModeText =
    configuration.register("filteringRegexModeText", "", INPUT, EXAMPLE_FILTERING_REGEX_MODE_TEXT)
  private val filteringRegexModeOptions = configuration.register("filteringRegexModeOptions", 0)
  private val filteringRegexModeErrorHolder = ErrorHolder()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(id = defaultSourceInputOutputHandlerId, exampleText = EXAMPLE_INPUT)
  }

  override fun Panel.buildSourceBottomConfigurationUi() {
    row { comboBox(TokenMode.entries).label("过滤:").bindItem(tokenMode) }
      .layout(RowLayout.PARENT_GRID)
      .topGap(TopGap.NONE)
      .bottomGap(BottomGap.NONE)

    buttonsGroup {
      row {
          cell()
          val containingFilteringModeRadioButton =
            radioButton("包含:").bind(filteringMode, FilteringMode.CONTAINING).gap(RightGap.SMALL)
          expandableTextField()
            .bindText(filteringContainingModeText)
            .enabledIf(containingFilteringModeRadioButton.selected)
            .resizableColumn()
            .align(Align.FILL)
        }
        .layout(RowLayout.PARENT_GRID)
        .bottomGap(BottomGap.NONE)

      row {
          cell()
          val containingFilteringModeRadioButton =
            radioButton("不包含:")
              .bind(filteringMode, FilteringMode.NOT_CONTAINING)
              .gap(RightGap.SMALL)
          expandableTextField()
            .bindText(filteringNotContainingModeText)
            .enabledIf(containingFilteringModeRadioButton.selected)
            .resizableColumn()
            .align(Align.FILL)
        }
        .layout(RowLayout.PARENT_GRID)
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          cell()
          radioButton("匹配正则表达式:").bind(filteringMode, FilteringMode.REGEX).gap(RightGap.SMALL)
          cell(RegexTextField(project, parentDisposable, filteringRegexModeText))
            .validationOnApply(filteringRegexModeErrorHolder.asValidation())
            .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
            .resizableColumn()
            .align(Align.FILL)
            .gap(RightGap.SMALL)
          cell(SelectRegexOptionsAction.createActionButton(filteringRegexModeOptions))
        }
        .layout(RowLayout.PARENT_GRID)
        .topGap(TopGap.NONE)
    }
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val tokenFilter: (String) -> Boolean =
      when (filteringMode.get()) {
        FilteringMode.CONTAINING -> {
          val filteringContainingModeTextValue = filteringContainingModeText.get()
          ({ it.contains(filteringContainingModeTextValue) })
        }

        FilteringMode.NOT_CONTAINING -> {
          val filteringNotContainingModeTextValue = filteringNotContainingModeText.get()
          ({ !it.contains(filteringNotContainingModeTextValue) })
        }

        FilteringMode.REGEX -> {
          val filteringRegexModeTextValue =
            try {
              Regex(filteringRegexModeText.get())
            } catch (e: Exception) {
              filteringRegexModeErrorHolder.add(e)
              return emptyByteArray
            }
          filteringRegexModeTextValue::matches
        }
      }

    val sourceText = String(source)
    return when (tokenMode.get()) {
      TokenMode.WORD -> {
        with(StringBuilder()) {
          var lastWasWord = false
          for (match in WORDS_SPLIT_REGEX.findAll(sourceText)) {
            val token = match.value
            if (token.isBlank() && !token.contains("\n")) {
              if (lastWasWord) {
                append(token)
              }
            } else if (token.contains("\n")) {
              append(token)
              lastWasWord = false
            } else if (tokenFilter(token)) {
              append(token)
              lastWasWord = true
            } else {
              lastWasWord = false
            }
          }
          toString().trim()
        }
      }

      TokenMode.LINE -> sourceText.lines().filter(tokenFilter).joinToString(System.lineSeparator())
    }.encodeToByteArray()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private enum class TokenMode(val pluralTitle: String) {

    WORD("单词"),
    LINE("行");

    override fun toString(): String = pluralTitle
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class FilteringMode {

    CONTAINING,
    NOT_CONTAINING,
    REGEX,
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextFilterTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "文本过滤器", contentTitle = "文本过滤器")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextFilterTransformer) = { configuration ->
      TextFilterTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val WORDS_SPLIT_REGEX = Regex("(\\S+|\\s+)")

    private val DEFAULT_TOKEN_SELECTION_MODE = TokenMode.LINE
    private val DEFAULT_FILTERING_MODE = FilteringMode.CONTAINING

    private const val EXAMPLE_FILTERING_CONTAINING_MODE_TEXT = "[error]"
    private const val EXAMPLE_FILTERING_NOT_CONTAINING_MODE_TEXT = "[info]"
    private const val EXAMPLE_FILTERING_REGEX_MODE_TEXT = "^\\[error\\].*$"

    private val EXAMPLE_INPUT =
      """
      [info] 应用程序已启动
      [error] 处理请求时出错
    """
        .trimIndent()
  }
}
