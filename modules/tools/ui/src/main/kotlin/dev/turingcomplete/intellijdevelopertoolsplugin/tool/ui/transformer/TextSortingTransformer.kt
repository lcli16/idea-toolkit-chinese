package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.common.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.LINE_BREAK

class TextSortingTransformer(
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
    title = "文本排序",
    sourceTitle = "无序",
    targetTitle = "排序",
    toTargetTitle = "排序",
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var unsortedSplitWordsDelimiter =
    configuration.register("unsortedPredefinedDelimiter", LINE_BREAK)
  private var unsortedIndividualSplitWordsDelimiter =
    configuration.register("unsortedIndividualSplitWordsDelimiter", " ")

  private var sortedJoinWordsDelimiter =
    configuration.register("sortedJoinWordsDelimiter", LINE_BREAK)
  private var sortedIndividualJoinWordsDelimiter =
    configuration.register("sortedIndividualJoinWordsDelimiter", " ")

  private var sortingOrder = configuration.register("sortingOrder", SortingOrder.LEXICOGRAPHIC)

  private var removeDuplicates = configuration.register("removeDuplicates", true)
  private var removeBlankWords = configuration.register("removeBlankWords", true)
  private var trimWords = configuration.register("trimWords", true)
  private var caseInsensitive = configuration.register("caseInsensitive", false)
  private var reverseOrder = configuration.register("reverseOrder", false)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_SOURCE_TEXT,
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val unsortedSplitWordsDelimiterPattern: Regex =
      unsortedSplitWordsDelimiter.get().splitPattern
        ?: Regex("${Regex.escape(unsortedIndividualSplitWordsDelimiter.get())}+")
    val sortedJoinWordsDelimiter =
      sortedJoinWordsDelimiter.get().joinDelimiter ?: sortedIndividualJoinWordsDelimiter.get()
    var unsortedWords = String(source).split(unsortedSplitWordsDelimiterPattern)

    if (trimWords.get()) {
      unsortedWords = unsortedWords.map { it.trim() }
    }
    if (removeDuplicates.get()) {
      unsortedWords = unsortedWords.distinct()
    }
    if (removeBlankWords.get()) {
      unsortedWords = unsortedWords.filter { it.isNotBlank() }
    }

    var comparator = sortingOrder.get().comparator
    if (caseInsensitive.get()) {
      comparator = comparator.makeCaseInsensitive()
    }
    if (reverseOrder.get()) {
      comparator = comparator.reversed()
    }
    unsortedWords = unsortedWords.sortedWith(comparator)

    return unsortedWords.joinToString(sortedJoinWordsDelimiter).encodeToByteArray()
  }

  override fun Panel.buildSourceBottomConfigurationUi() {
    row {
      buildSplitConfigurationUi(
        "将未排序的单词拆分:",
        unsortedSplitWordsDelimiter,
        unsortedIndividualSplitWordsDelimiter,
      )
    }

    row {
      buildSplitConfigurationUi(
        "连接排序的单词:",
        sortedJoinWordsDelimiter,
        sortedIndividualJoinWordsDelimiter,
      )
    }

    row {
      comboBox(SortingOrder.entries).label("顺序:").bindItem(sortingOrder)
      checkBox("反向").bindSelected(reverseOrder)
      checkBox("不区分大小写").bindSelected(caseInsensitive)
    }

    row {
      checkBox("删除重复项").bindSelected(removeDuplicates)
      checkBox("剪裁单词").bindSelected(trimWords)
      checkBox("删除空白单词").bindSelected(removeBlankWords)
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun Row.buildSplitConfigurationUi(
    title: String,
    splitWordsDelimiter: ObservableMutableProperty<WordsDelimiter>,
    individualDelimiter: ObservableMutableProperty<String>,
  ) {
    val splitWordsDelimiterComboBox =
      comboBox(WordsDelimiter.entries).label(title).bindItem(splitWordsDelimiter).component
    textField()
      .bindText(individualDelimiter)
      .visibleIf(ComboBoxPredicate(splitWordsDelimiterComboBox) { it == INDIVIDUAL })
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class SortingOrder(private val title: String, val comparator: Comparator<String>) {

    NATURAL("自然", NaturalComparator()),
    LEXICOGRAPHIC("字典", { a, b -> a.compareTo(b) }),
    WORD_LENGTH("字长", Comparator { a, b -> a.length - b.length });

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class WordsDelimiter(
    private val title: String,
    val splitPattern: Regex?,
    val joinDelimiter: String?,
  ) {

    LINE_BREAK("换行", Regex("\\R+"), System.lineSeparator()),
    SPACE("空格", Regex("\\s+"), " "),
    COMMA("逗号", Regex(",+"), ","),
    SEMICOLON("分号", Regex(";+"), ";"),
    DASH("破折号", Regex("-+"), "-"),
    UNDERSCORE("下划线", Regex("_+"), "_"),
    INDIVIDUAL("单个", null, null);

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextSortingTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "文本排序", contentTitle = "文本排序")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextSortingTransformer) = { configuration ->
      TextSortingTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val EXAMPLE_SOURCE_TEXT = "b\nc\na"
  }
}
