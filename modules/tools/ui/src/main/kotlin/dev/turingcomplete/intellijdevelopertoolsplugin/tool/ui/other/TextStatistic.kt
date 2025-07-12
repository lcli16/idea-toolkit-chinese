package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextStatisticUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.SimpleTable
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.simpleColumnInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.TextStatistic.OpenTextStatisticContext
import javax.swing.SortOrder
import org.apache.commons.text.StringEscapeUtils

class TextStatistic(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable), OpenDeveloperToolHandler<OpenTextStatisticContext> {
  // -- Properties ---------------------------------------------------------- //

  private val text = configuration.register("text", "", INPUT, TEXT_EXAMPLE)

  private lateinit var metricsTable: SimpleTable<TextMetric>
  private lateinit var uniqueWordsTable: SimpleTable<Pair<String, Int>>
  private lateinit var uniqueCharactersTable: SimpleTable<Pair<Char, Int>>

  private val charactersCounter = TextMetric("字符")
  private val wordsCounter = TextMetric("单词")
  private val uniqueWordsCounter = TextMetric("唯一单词")
  private val averageWordLengthCounter = TextMetric("平均字长")
  private val sentencesCounter = TextMetric("句子")
  private val averageWordsPerSentenceCounter = TextMetric("每句平均字数")
  private val paragraphsCounter = TextMetric("段落")
  private val uniqueCharactersCounter = TextMetric("唯一字符")
  private val lettersCounter = TextMetric("字母")
  private val digitsCounter = TextMetric("数字")
  private val nonAsciiCharactersCounter = TextMetric("非 ASCII 字符")
  private val isoControlCharactersCounter = TextMetric("ISO 控制字符")
  private val whitespacesCounter = TextMetric("空格")
  private val lineBreaksCounter = TextMetric("换行符")
  private val uniqueCharacters = mutableListOf<Pair<Char, Int>>()
  private val uniqueWords = mutableListOf<Pair<String, Int>>()

  private val counterAlarm by lazy { Alarm(parentDisposable) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        cell(
            Splitter(true, 0.75f).apply {
              firstComponent = createInputEditorComponent()
              secondComponent = createMetricsComponent()
            }
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  override fun afterBuildUi() {
    updateCounter()
  }

  override fun applyOpenDeveloperToolContext(context: OpenTextStatisticContext) {
    text.set(context.text)
    updateCounter()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createInputEditorComponent() =
    AdvancedEditor(
        id = "text",
        context = context,
        configuration = configuration,
        project = project,
        title = "Text",
        editorMode = EditorMode.INPUT,
        parentDisposable = parentDisposable,
        textProperty = text,
      )
      .apply { onTextChangeFromUi { counterAlarm.addRequest({ updateCounter() }, 100) } }
      .component

  private fun createMetricsComponent() = panel {
    row {
        cell(
            JBTabbedPane().apply {
              metricsTable = createMetricsTable()
              addTab("指标", ScrollPaneFactory.createScrollPane(metricsTable, false))

              uniqueWordsTable = createUniqueWordsTable()
              addTab("唯一单词", ScrollPaneFactory.createScrollPane(uniqueWordsTable, false))

              uniqueCharactersTable = createUniqueCharactersTable()
              addTab("唯一字符", ScrollPaneFactory.createScrollPane(uniqueCharactersTable, false))
            }
          )
          .resizableColumn()
          .align(Align.FILL)
      }
      .resizableRow()
  }

  private fun createMetricsTable() =
    SimpleTable(
      items =
        listOf(
          charactersCounter,
          wordsCounter,
          uniqueWordsCounter,
          averageWordLengthCounter,
          sentencesCounter,
          averageWordsPerSentenceCounter,
          paragraphsCounter,
          uniqueCharactersCounter,
          lettersCounter,
          digitsCounter,
          nonAsciiCharactersCounter,
          isoControlCharactersCounter,
          whitespacesCounter,
          lineBreaksCounter,
        ),
      columns =
        listOf(
          simpleColumnInfo("名称", { it.title }) { it.title },
          simpleColumnInfo("统计", { it.value }) { it.value },
        ),
      toCopyValue = { "${it.title}: ${it.value}" },
      initialSortedColumn = 0 to SortOrder.UNSORTED,
    )

  private fun createUniqueWordsTable(): SimpleTable<Pair<String, Int>> =
    SimpleTable(
      items = uniqueWords,
      columns =
        listOf(
          simpleColumnInfo("名称", { it.first }, { it.first }),
          simpleColumnInfo("统计", { it.second.toString() }, { it.second }),
        ),
      toCopyValue = { it.first },
      initialSortedColumn = 1 to SortOrder.DESCENDING,
    )

  private fun createUniqueCharactersTable(): SimpleTable<Pair<Char, Int>> {
    val characterToDisplay: (Pair<Char, Int>) -> String = { (character, _) ->
      if (Character.isISOControl(character)) {
        when (character) {
          '\b' -> "\\b"
          '\t' -> "\\t"
          '\n' -> "\\n"
          '\u000c' -> "\\f"
          '\r' -> "\\r"
          else -> "\\u" + String.format("%04x", character.code)
        }
      } else if (character == ' ') {
        "Whitespace"
      } else if (character.code > 127) {
        "$character (${StringEscapeUtils.escapeJava(character.toString())})"
      } else {
        character.toString()
      }
    }
    return SimpleTable(
      items = uniqueCharacters,
      columns =
        listOf(
          simpleColumnInfo("名称", characterToDisplay, characterToDisplay),
          simpleColumnInfo("统计", { it.second.toString() }, { it.second }),
        ),
      toCopyValue = { it.first.toString() },
      initialSortedColumn = 1 to SortOrder.DESCENDING,
    )
  }

  private fun updateCounter() {
    with(TextStatisticUtils.gatherStatistic(text.get())) {
      charactersCounter.value = charactersCount.toString()
      wordsCounter.value = wordsCount.toString()
      uniqueWordsCounter.value = uniqueWords.size.toString()
      averageWordLengthCounter.value = "%.2f".format(averageWordLength)
      sentencesCounter.value = sentencesCount.toString()
      averageWordsPerSentenceCounter.value = "%.2f".format(averageWordsPerSentence)
      paragraphsCounter.value = paragraphsCount.toString()
      uniqueCharactersCounter.value = uniqueCharacters.size.toString()
      lettersCounter.value = lettersCount.toString()
      digitsCounter.value = digitsCount.toString()
      nonAsciiCharactersCounter.value = nonAsciiCharactersCount.toString()
      isoControlCharactersCounter.value = isoControlCharactersCount.toString()
      whitespacesCounter.value = whitespacesCount.toString()
      lineBreaksCounter.value = lineBreaksCount.toString()
      this@TextStatistic.uniqueWords.clear()
      this@TextStatistic.uniqueWords.addAll(uniqueWords.toList())
      this@TextStatistic.uniqueCharacters.clear()
      this@TextStatistic.uniqueCharacters.addAll(uniqueCharacters.toList())
    }
    metricsTable.reload()
    uniqueWordsTable.reload()
    uniqueCharactersTable.reload()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class TextMetric(val title: String, var value: String = "Unknown")

  // -- Inner Type ---------------------------------------------------------- //

  data class OpenTextStatisticContext(val text: String) : OpenDeveloperToolContext

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextStatistic> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "文本统计", contentTitle = "文本统计")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextStatistic) = { configuration ->
      TextStatistic(
        context = context,
        configuration = configuration,
        project = project,
        parentDisposable = parentDisposable,
      )
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val ID = "text-statistic"

    private val TEXT_EXAMPLE =
      """
在遥远的山这个词后面，远离 Vokalia 和 Consonantia 国家，那里住着盲文。

他们分开后住在 Bookmarksgrove，就在 Semantics 的海岸上，这是一片巨大的语言海洋。

一条名叫 Duden 的小河流流过他们的地方，并为它提供必要的王冠。这是一个天堂般的国家，在这里，烤过的句子部分飞进你的嘴里。
"""
        .trimIndent()

    val openTextStatisticReference =
      OpenDeveloperToolReference.of(ID, OpenTextStatisticContext::class)
  }
}
