package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import com.google.zxing.pdf417.encoder.Compaction
import com.google.zxing.pdf417.encoder.Dimensions
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.QRCode
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColorChooserService
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.MAX_LINE_LENGTH_WORD_WRAP
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.actionsButton
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ValidateMinIntValueSide
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.toJBColor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateMinMaxValueRelation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.BarcodeGenerator.ErrorCorrectionSupport.LEVEL_BITS
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.BarcodeGenerator.ErrorCorrectionSupport.LEVEL_ENUM_NAME
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.BarcodeGenerator.ErrorCorrectionSupport.UNSUPPORTED
import java.awt.Container
import java.awt.Graphics
import java.lang.Integer.toHexString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.border.LineBorder

class BarcodeGenerator
private constructor(
  private val formats: Map<Format, FormatConfiguration>,
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private var liveGeneration = configuration.register("liveGeneration", true)
  private val format = configuration.register("format", DEFAULT_FORMAT)
  private val contentText = configuration.register("contentText", "", INPUT, EXAMPLE_CONTENT)

  private val contentEditor by lazy { createContentEditor(parentDisposable) }
  private val contentErrorHolder = ErrorHolder()

  private val generateAlarm by lazy { Alarm(parentDisposable) }

  private val drawPanel by lazy { DrawPanel(configuration) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    lateinit var formatComboBox: ComboBox<Format>
    row {
      formatComboBox =
        comboBox(Format.entries)
          .label("格式:")
          .bindItem(format)
          .whenItemSelectedFromUi { generate() }
          .component
    }

    row {
        cell(contentEditor.component)
          .validationOnApply(contentEditor.bindValidator(contentErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
      }
      .layout(RowLayout.INDEPENDENT)

    formats.forEach { (format, configuration) ->
      with(configuration) {
        buildConfigurationUi(ComboBoxPredicate(formatComboBox) { it == format }) { generate() }
      }
    }

    row {
      label("背景颜色:").gap(RightGap.SMALL)
      cell(ColorPanel(drawPanel.backgroundColor)).gap(RightGap.SMALL)
      lateinit var backgroundColorButton: JButton
      backgroundColorButton =
        button("修改") {
            ColorChooserService.instance
              .showDialog(
                project,
                backgroundColorButton,
                "选择背景颜色",
                drawPanel.backgroundColor.get(),
              )
              ?.let {
                drawPanel.backgroundColor.set(it.toJBColor())
                generate()
              }
          }
          .component

      label("前景色:").gap(RightGap.SMALL)
      cell(ColorPanel(drawPanel.foregroundColor)).gap(RightGap.SMALL)
      lateinit var foregroundColorButton: JButton
      foregroundColorButton =
        button("修改") {
            ColorChooserService.instance
              .showDialog(
                project,
                foregroundColorButton,
                "选择前景色",
                drawPanel.foregroundColor.get(),
              )
              ?.let {
                drawPanel.foregroundColor.set(it.toJBColor())
                generate()
              }
          }
          .component
    }

    row {
      val liveGenerationCheckBox =
        checkBox("实时生成")
          .bindSelected(liveGeneration)
          .whenStateChangedFromUi {
            if (it) {
              generate()
            }
          }
          .gap(RightGap.SMALL)

      button("▼ 生成") { generate(false) }.enabledIf(liveGenerationCheckBox.selected.not())

      val exportToFileActions =
        ImageIO.getWriterFileSuffixes()
          .map { fileFormat ->
            DumbAwareAction.create("导出为 $fileFormat") { exportToFile(fileFormat) }
          }
          .toTypedArray()
      actionsButton(
          actions = exportToFileActions,
          actionPlace = BarcodeGenerator::class.java.name,
          icon = AllIcons.Actions.MenuSaveall,
        )
        .label("导出:")
        .visibleIf(contentErrorHolder.asComponentPredicate().not())
    }

    row {
        cell(ScrollPaneFactory.createScrollPane(drawPanel, true))
          .label("生成的图像:", LabelPosition.TOP)
          .align(AlignY.TOP)
      }
      .visibleIf(contentErrorHolder.asComponentPredicate().not())
      .resizableRow()
  }

  override fun afterBuildUi() {
    generate(liveGeneration.get())
  }

  override fun reset() {
    generate(liveGeneration.get())
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun generate(fromConfigChange: Boolean = true) {
    if (configuration.isResetting || (fromConfigChange && !liveGeneration.get())) {
      return
    }

    if (!isDisposed && !generateAlarm.isDisposed) {
      generateAlarm.cancelAllRequests()
      generateAlarm.addRequest({ doGenerate() }, 100)
    }
  }

  private fun doGenerate(): List<ValidationInfo> {
    contentErrorHolder.clear()
    try {
      val formatConfiguration = formats[format.get()]!!
      val widthValue = formatConfiguration.width()
      val heightValue = formatConfiguration.height()

      val newMatrix =
        multiFormatWriter.encode(
          contentEditor.text,
          formatConfiguration.barcodeFormat,
          widthValue,
          heightValue,
          formatConfiguration.createHints(),
        )
      drawPanel.matrix.set(newMatrix)
    } catch (_: NumberFormatException) {
      contentErrorHolder.add("输入必须为数字")
    } catch (e: Exception) {
      contentErrorHolder.add(e)
    }

    return validate()
  }

  private fun createContentEditor(parentDisposable: Disposable) =
    AdvancedEditor(
        "content",
        context,
        configuration,
        project,
        "内容",
        AdvancedEditor.EditorMode.INPUT,
        parentDisposable,
        contentText,
      )
      .onTextChangeFromUi { generate() }

  private fun exportToFile(fileFormat: String) {
    val fileSaverDescriptor = FileSaverDescriptor("导出为 $fileFormat", "ExportToFile.$fileFormat",fileFormat,"jpeg")
    val timeStamp = LocalDateTime.now().format(timestampFormat)
    val defaultFilename = "${format.get().name.lowercase()}_$timeStamp.$fileFormat"
    FileChooserFactory.getInstance()
      .createSaveFileDialog(fileSaverDescriptor, project)
      .save(defaultFilename)
      ?.file
      ?.toPath()
      ?.let { targetPath ->
        MatrixToImageWriter.writeToPath(drawPanel.matrix.get(), fileFormat, targetPath)
      }

  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class Format(
    val title: String,
    val createConfiguration: (DeveloperToolConfiguration) -> FormatConfiguration,
  ) {

    AZTEC("Aztec 2D", { configuration -> AztecCodeConfiguration(configuration) }),
    QR_CODE("QR Code 2D", { configuration -> QrCodeConfiguration(configuration) }),
    DATA_MATRIX("Data Matrix 2D", { configuration -> DataMatrixConfiguration(configuration) }),
    PDF_417("PDF417", { configuration -> Pdf417FormatConfiguration(configuration) }),
    CODE_39(
      "Code 39 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.CODE_39,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    CODE_93(
      "Code 93 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.CODE_93,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    CODE_128(
      "Code 128 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.CODE_128,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    EAN_8(
      "EAN-8 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.EAN_8,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    EAN_13(
      "EAN-13 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.EAN_13,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    ITF(
      "ITF (Interleaved Two of Five) 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.ITF,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    CODABAR(
      "CODABAR 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.CODABAR,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
          supportsMargin = true,
          defaultMargin = 10,
        )
      },
    ),
    UPC_A(
      "UPC-A 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.UPC_A,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
        )
      },
    ),
    UPC_E(
      "UPC-E 1D",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.UPC_E,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
        )
      },
    ),
    UPC_EAN_EXTENSION(
      "UPC/EAN extension",
      { configuration ->
        FormatConfiguration(
          barcodeFormat = BarcodeFormat.UPC_EAN_EXTENSION,
          configuration = configuration,
          supportsHeight = true,
          defaultHeight = 50,
        )
      },
    );

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  private open class FormatConfiguration(
    val barcodeFormat: BarcodeFormat,
    configuration: DeveloperToolConfiguration,
    private val supportsWidth: Boolean = false,
    defaultWidth: Int = 250,
    private val supportsHeight: Boolean = false,
    defaultHeight: Int = 250,
    private val supportsMargin: Boolean = false,
    defaultMargin: Int = 1,
    private val supportsErrorCorrection: ErrorCorrectionSupport = UNSUPPORTED,
    defaultErrorCorrection: ErrorCorrection = ErrorCorrection.H,
    private val comment: String? = null,
  ) {

    private val width = configuration.register("${barcodeFormat.name}-width", defaultWidth)
    private val height = configuration.register("${barcodeFormat.name}-height", defaultHeight)
    private val margin = configuration.register("${barcodeFormat.name}-margin", defaultMargin)
    private val errorCorrection =
      configuration.register("${barcodeFormat.name}-errorCorrection", defaultErrorCorrection)

    fun width() = width.get()

    fun height() = height.get()

    fun Panel.buildConfigurationUi(visible: ComponentPredicate, onConfigurationChange: () -> Unit) {
      row {
          if (supportsWidth) {
            textField()
              .label("宽度:")
              .bindIntTextImproved(width)
              .validateLongValue(LongRange(10, 1000))
              .columns(COLUMNS_TINY)
              .whenTextChangedFromUi { onConfigurationChange() }
          }

          if (supportsHeight) {
            textField()
              .label("高度:")
              .bindIntTextImproved(height)
              .validateLongValue(LongRange(1, 1000))
              .columns(COLUMNS_TINY)
              .whenTextChangedFromUi { onConfigurationChange() }
          }

          if (supportsMargin) {
            textField()
              .label("边距:")
              .bindIntTextImproved(margin)
              .validateLongValue(LongRange(0, 100))
              .columns(COLUMNS_TINY)
              .whenTextChangedFromUi { onConfigurationChange() }
          }

          if (supportsErrorCorrection != UNSUPPORTED) {
            comboBox(ErrorCorrection.entries).label("纠错:").bindItem(errorCorrection).onChanged {
              onConfigurationChange()
            }
          }

          if (comment != null) {
            rowComment(comment = comment, maxLineLength = MAX_LINE_LENGTH_WORD_WRAP)
          }
        }
        .visibleIf(visible)

      buildAdditionalConfigurationUi(visible, onConfigurationChange)
    }

    open fun Panel.buildAdditionalConfigurationUi(
      visible: ComponentPredicate,
      onConfigurationChange: () -> Unit,
    ) {}

    open fun createHints(): Map<EncodeHintType, Any> {
      val hints = mutableMapOf<EncodeHintType, Any>()
      hints[EncodeHintType.CHARACTER_SET] = "utf-8"
      if (supportsErrorCorrection == LEVEL_ENUM_NAME) {
        hints[EncodeHintType.ERROR_CORRECTION] = errorCorrection.get().level.name
      } else if (supportsErrorCorrection == LEVEL_BITS) {
        hints[EncodeHintType.ERROR_CORRECTION] = errorCorrection.get().level.bits
      }

      if (supportsMargin) {
        hints[EncodeHintType.MARGIN] = margin.get()
      }

      return hints
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class ErrorCorrectionSupport {

    UNSUPPORTED,
    LEVEL_ENUM_NAME,
    LEVEL_BITS,
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class Pdf417FormatConfiguration(configuration: DeveloperToolConfiguration) :
    FormatConfiguration(
      barcodeFormat = BarcodeFormat.PDF_417,
      configuration = configuration,
      supportsWidth = true,
      defaultWidth = 250,
      supportsHeight = true,
      defaultHeight = 50,
      supportsMargin = true,
      defaultMargin = 5,
      supportsErrorCorrection = LEVEL_BITS,
      comment =
        "实际大小将根据比例因子计算，该比例因子基于宽度和高度值之间的关系。使用大于 宽度 的 高度 值来旋转条形码。",
    ) {

    private val useCompactMode =
      configuration.register("${BarcodeFormat.PDF_417}-compactMode", false)
    private val compactModeType =
      configuration.register("${BarcodeFormat.PDF_417}-compactionModeType", Compaction.AUTO)
    private val insertEcis = configuration.register("${BarcodeFormat.PDF_417}-insertEcis", false)
    private val setDimensions =
      configuration.register("${BarcodeFormat.PDF_417}-setDimensions", false)
    private val minColumns = configuration.register("${BarcodeFormat.PDF_417}-minColumns", 1)
    private val maxColumns = configuration.register("${BarcodeFormat.PDF_417}-maxColumns", 50)
    private val minRows = configuration.register("${BarcodeFormat.PDF_417}-minRows", 1)
    private val maxRows = configuration.register("${BarcodeFormat.PDF_417}-minRows", 50)

    @Suppress("UnstableApiUsage")
    override fun Panel.buildAdditionalConfigurationUi(
      visible: ComponentPredicate,
      onConfigurationChange: () -> Unit,
    ) {
      row {
          val useCompactModeCheckBox =
            checkBox("PDF417 紧凑模式:")
              .bindSelected(useCompactMode)
              .onChanged { onConfigurationChange() }
              .gap(RightGap.SMALL)

          val compactionRenderer =
            textListCellRenderer<Compaction?> {
              it?.name?.lowercase()?.replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
                ?: throw IllegalArgumentException()
            }
          comboBox(Compaction.entries, compactionRenderer)
            .bindItem(compactModeType)
            .whenItemSelectedFromUi { onConfigurationChange() }
            .enabledIf(useCompactModeCheckBox.selected)

          checkBox("自动插入 ECI").bindSelected(insertEcis).whenStateChangedFromUi {
            onConfigurationChange()
          }
        }
        .visibleIf(visible)

      lateinit var limitDimensionsCheckbox: Cell<JBCheckBox>
      row {
          limitDimensionsCheckbox =
            checkBox("限制尺寸:").bindSelected(setDimensions).whenStateChangedFromUi {
              onConfigurationChange()
            }
        }
        .visibleIf(visible)
      row {
          textField()
            .label("最小列.:")
            .bindIntTextImproved(minColumns)
            .validateLongValue(LongRange(1, 1000))
            .validateMinMaxValueRelation(ValidateMinIntValueSide.MIN) { maxColumns.get() }
            .columns(COLUMNS_TINY)
            .gap(RightGap.SMALL)
            .whenTextChangedFromUi { onConfigurationChange() }
          textField()
            .label("最大. 列.:")
            .bindIntTextImproved(maxColumns)
            .validateLongValue(LongRange(1, 1000))
            .validateMinMaxValueRelation(ValidateMinIntValueSide.MAX) { minColumns.get() }
            .columns(COLUMNS_TINY)
            .whenTextChangedFromUi { onConfigurationChange() }

          textField()
            .label("最小. 行:")
            .bindIntTextImproved(minRows)
            .validateLongValue(LongRange(1, 1000))
            .validateMinMaxValueRelation(ValidateMinIntValueSide.MIN) { maxRows.get() }
            .columns(COLUMNS_TINY)
            .gap(RightGap.SMALL)
            .whenTextChangedFromUi { onConfigurationChange() }
          textField()
            .label("最大. 行:")
            .bindIntTextImproved(maxRows)
            .validateLongValue(LongRange(1, 1000))
            .validateMinMaxValueRelation(ValidateMinIntValueSide.MAX) { minRows.get() }
            .columns(COLUMNS_TINY)
            .whenTextChangedFromUi { onConfigurationChange() }
        }
        .visibleIf(visible)
        .enabledIf(limitDimensionsCheckbox.selected)
    }

    override fun createHints(): Map<EncodeHintType, Any> {
      val hints = super.createHints().toMutableMap()

      hints[EncodeHintType.PDF417_COMPACT] = useCompactMode.get()
      hints[EncodeHintType.PDF417_COMPACTION] = compactModeType.get()
      hints[EncodeHintType.PDF417_AUTO_ECI] = insertEcis.get()

      if (setDimensions.get()) {
        hints[EncodeHintType.PDF417_DIMENSIONS] =
          Dimensions(minColumns.get(), maxColumns.get(), minRows.get(), maxRows.get())
      }

      return hints
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class AztecCodeConfiguration(configuration: DeveloperToolConfiguration) :
    FormatConfiguration(
      barcodeFormat = BarcodeFormat.AZTEC,
      configuration = configuration,
      supportsWidth = true,
      defaultWidth = 250,
      supportsHeight = true,
      defaultHeight = 250,
      supportsErrorCorrection = LEVEL_BITS,
    ) {

    private val layers = configuration.register("${BarcodeFormat.AZTEC}-layers", 0)

    @Suppress("UnstableApiUsage")
    override fun Panel.buildAdditionalConfigurationUi(
      visible: ComponentPredicate,
      onConfigurationChange: () -> Unit,
    ) {
      row {
          val renderer =
            textListCellRenderer<Int?> {
              when {
                it == null -> throw IllegalArgumentException()
                it < 0 -> "$it (compact)"
                it == 0 -> "最低"
                else -> it.toString()
              }
            }
          comboBox(IntRange(-4, 32).toList(), renderer)
            .label("层:")
            .bindItem(layers)
            .whenItemSelectedFromUi { onConfigurationChange() }
        }
        .visibleIf(visible)
    }

    override fun createHints(): Map<EncodeHintType, Any> =
      super.createHints() + (EncodeHintType.AZTEC_LAYERS to layers.get())
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class QrCodeConfiguration(configuration: DeveloperToolConfiguration) :
    FormatConfiguration(
      barcodeFormat = BarcodeFormat.QR_CODE,
      configuration = configuration,
      supportsWidth = true,
      defaultWidth = 200,
      supportsHeight = true,
      defaultHeight = 200,
      supportsMargin = true,
      defaultMargin = 0,
      supportsErrorCorrection = LEVEL_ENUM_NAME,
    ) {

    private val version = configuration.register("${BarcodeFormat.QR_CODE}-version", 0)
    private val compactMode = configuration.register("${BarcodeFormat.QR_CODE}-compactMode", false)
    private val gs1 = configuration.register("${BarcodeFormat.QR_CODE}-gs1", false)
    private val maskPattern = configuration.register("${BarcodeFormat.QR_CODE}-maskPattern", -1)

    @Suppress("UnstableApiUsage")
    override fun Panel.buildAdditionalConfigurationUi(
      visible: ComponentPredicate,
      onConfigurationChange: () -> Unit,
    ) {
      row {
          val compactModeCheckBox =
            checkBox("紧凑模式")
              .bindSelected(compactMode)
              .whenStateChangedFromUi { onConfigurationChange() }
              .gap(RightGap.SMALL)

          checkBox("使用 GS1")
            .bindSelected(gs1)
            .whenStateChangedFromUi { onConfigurationChange() }
            .enabledIf(compactModeCheckBox.selected)

          val versionRenderer =
            textListCellRenderer<Int?> {
              if (it == 0) "最低" else it?.toString() ?: throw IllegalArgumentException()
            }
          comboBox(IntRange(0, 40).toList(), versionRenderer)
            .label("版本:")
            .bindItem(version)
            .whenItemSelectedFromUi { onConfigurationChange() }

          val maskPatternRenderer =
            textListCellRenderer<Int?> {
              if (it == -1) "Best" else it?.toString() ?: throw IllegalArgumentException()
            }
          comboBox(IntRange(-1, QRCode.NUM_MASK_PATTERNS - 1).toList(), maskPatternRenderer)
            .label("蒙版图案:")
            .bindItem(maskPattern)
            .whenItemSelectedFromUi { onConfigurationChange() }
        }
        .visibleIf(visible)
    }

    override fun createHints(): Map<EncodeHintType, Any> {
      val hints = super.createHints().toMutableMap()

      if (compactMode.get()) {
        hints + (EncodeHintType.QR_COMPACT to true)
        hints + (EncodeHintType.GS1_FORMAT to gs1.get())
      }
      if (version.get() != 0) {
        hints + (EncodeHintType.QR_VERSION to version.get())
      }
      if (maskPattern.get() != -1) {
        hints + (EncodeHintType.QR_MASK_PATTERN to maskPattern.get())
      }

      return hints
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class DataMatrixConfiguration(configuration: DeveloperToolConfiguration) :
    FormatConfiguration(
      barcodeFormat = BarcodeFormat.DATA_MATRIX,
      configuration = configuration,
      supportsWidth = true,
      defaultWidth = 250,
      supportsHeight = true,
      defaultHeight = 250,
    ) {

    private val compactMode =
      configuration.register("${BarcodeFormat.DATA_MATRIX}-compactMode", false)
    private val gs1 = configuration.register("${BarcodeFormat.DATA_MATRIX}-gs1", false)
    private val forceC40 = configuration.register("${BarcodeFormat.DATA_MATRIX}-forceC40", false)
    private val symbolShape =
      configuration.register("${BarcodeFormat.DATA_MATRIX}-symbolShape", SymbolShapeHint.FORCE_NONE)

    @Suppress("UnstableApiUsage")
    override fun Panel.buildAdditionalConfigurationUi(
      visible: ComponentPredicate,
      onConfigurationChange: () -> Unit,
    ) {
      row {
          val compactModeCheckBox =
            checkBox("紧凑模式")
              .bindSelected(compactMode)
              .whenStateChangedFromUi { onConfigurationChange() }
              .gap(RightGap.SMALL)

          checkBox("使用 GS1")
            .bindSelected(gs1)
            .whenStateChangedFromUi { onConfigurationChange() }
            .enabledIf(compactModeCheckBox.selected)
            .gap(RightGap.SMALL)

          checkBox("强制 C40")
            .bindSelected(forceC40)
            .whenStateChangedFromUi { onConfigurationChange() }
            .enabledIf(compactModeCheckBox.selected.not())

          val symbolShapeRenderer =
            textListCellRenderer<SymbolShapeHint?> {
              when (it) {
                SymbolShapeHint.FORCE_NONE -> "自动"
                SymbolShapeHint.FORCE_SQUARE -> "正方形"
                SymbolShapeHint.FORCE_RECTANGLE -> "矩形"
                else -> throw IllegalArgumentException()
              }
            }
          comboBox(SymbolShapeHint.entries, symbolShapeRenderer)
            .bindItem(symbolShape)
            .label("符号形状:")
            .whenItemSelectedFromUi { onConfigurationChange() }
        }
        .visibleIf(visible)
    }

    override fun createHints(): Map<EncodeHintType, Any> =
      super.createHints() +
        mapOf(
          EncodeHintType.DATA_MATRIX_COMPACT to compactMode,
          EncodeHintType.DATA_MATRIX_SHAPE to symbolShape.get(),
          EncodeHintType.GS1_FORMAT to gs1.get(),
          EncodeHintType.FORCE_C40 to forceC40.get(),
        )
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class ErrorCorrection(val level: ErrorCorrectionLevel, val title: String) {

    L(ErrorCorrectionLevel.L, "~7%"),
    M(ErrorCorrectionLevel.M, "~17%"),
    Q(ErrorCorrectionLevel.Q, "~25%"),
    H(ErrorCorrectionLevel.H, "~30%");

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class DrawPanel(configuration: DeveloperToolConfiguration) : JPanel() {

    val matrix: ObservableMutableProperty<BitMatrix> = AtomicProperty(BitMatrix(200))

    val backgroundColor = configuration.register("backgroundColor", DEFAULT_BACKGROUND_COLOR)
    val foregroundColor = configuration.register("foregroundColor", DEFAULT_FOREGROUND_COLOR)

    init {
      matrix.afterChange {
        val size = JBUI.size(it.width, it.height)
        minimumSize = size
        preferredSize = size
        maximumSize = size

        revalidate2(this)

        repaint()
        parent?.repaint()
      }
      backgroundColor.afterChange {
        revalidate()
        repaint()
      }
      foregroundColor.afterChange {
        revalidate()
        repaint()
      }
    }

    fun revalidate2(parent: Container) {
      parent.revalidate()
      parent.parent?.let { revalidate2(it) }
    }

    override fun paint(g: Graphics) {
      super.paint(g)

      val qrCodeMatrixValue = matrix.get()

      g.color = backgroundColor.get()
      g.fillRect(0, 0, qrCodeMatrixValue.width, qrCodeMatrixValue.height)

      g.color = foregroundColor.get()
      for (i in 0 until qrCodeMatrixValue.width) {
        for (j in 0 until qrCodeMatrixValue.height) {
          if (qrCodeMatrixValue[i, j]) {
            g.fillRect(i, j, 1, 1)
          }
        }
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ColorPanel(private val color: ObservableMutableProperty<JBColor>) : JPanel() {

    init {
      val size = JBUI.size(15, 15)
      minimumSize = size
      preferredSize = size
      maximumSize = size
      border = LineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())

      updateToolTipText()
      color.afterChange {
        updateToolTipText()
        repaint()
      }
    }

    override fun paint(g: Graphics) {
      super.paint(g)

      g.color = color.get()
      g.fillRect(0, 0, size.width, size.height)

      border.paintBorder(this, g, 0, 0, size.width, size.height)
    }

    private fun updateToolTipText() {
      val colorValue = color.get()
      toolTipText =
        "#${toHexString(colorValue.red)}${toHexString(colorValue.green)}${toHexString(colorValue.blue)}"
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<BarcodeGenerator> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "二维码/条形码", contentTitle = "二维码/条形码生成器")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> BarcodeGenerator) = { configuration ->
      val formats: Map<Format, FormatConfiguration> =
        Format.entries.associateWith { it.createConfiguration(configuration) }
      BarcodeGenerator(formats, context, configuration, project, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val multiFormatWriter = MultiFormatWriter()

    private const val EXAMPLE_CONTENT = "Lorem ipsum dolor sit amet."
    private val DEFAULT_FORMAT = Format.QR_CODE

    private val DEFAULT_BACKGROUND_COLOR: JBColor = JBColor.WHITE
    private val DEFAULT_FOREGROUND_COLOR: JBColor = JBColor.BLACK

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }
}
