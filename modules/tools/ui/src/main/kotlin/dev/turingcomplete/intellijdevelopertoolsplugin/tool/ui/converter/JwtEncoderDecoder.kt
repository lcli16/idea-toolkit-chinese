package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.TextRange
import com.intellij.ui.IconManager
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import com.intellij.util.ExceptionUtil
import com.intellij.util.text.DateFormatUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.capitalize
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeBase64String
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.createSensitiveInputsHandlingToolTipText
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.SimpleToggleAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.registerDynamicToolTip
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.setValidationResultBorder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.ChangeOrigin.ENCODED
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.ChangeOrigin.HEADER_OR_PAYLOAD
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.ChangeOrigin.SIGNATURE_CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SecretKeyEncodingMode.BASE32
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SecretKeyEncodingMode.BASE64
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SecretKeyEncodingMode.RAW
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SignatureAlgorithm.HMAC256
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SignatureAlgorithmKind.ECDSA
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SignatureAlgorithmKind.HMAC
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder.SignatureAlgorithmKind.RSA
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Objects
import java.util.StringJoiner
import javax.swing.Icon
import javax.swing.JComponent
import org.apache.commons.codec.binary.Base32
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA512
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA512
import org.jose4j.jws.JsonWebSignature
import org.jose4j.keys.HmacKey

class JwtEncoderDecoder(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private var liveConversion = configuration.register("liveConversion", true)
  private var encodedText = configuration.register("encodedText", "", INPUT, EXAMPLE_ENCODED)
  private var headerText = configuration.register("headerText", "", INPUT, EXAMPLE_HEADER)
  private var payloadText = configuration.register("payloadText", "", INPUT, EXAMPLE_PAYLOAD)

  private val highlightEncodedAlarm by lazy { Alarm(parentDisposable) }
  private val highlightHeaderAlarm by lazy { Alarm(parentDisposable) }
  private val highlightPayloadAlarm by lazy { Alarm(parentDisposable) }
  private val conversionAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput: AdvancedEditor? = null
  private val encodedEditor by lazy { createEncodedEditor() }
  private val headerEditor by lazy { createHeaderEditor() }
  private val payloadEditor by lazy { createPayloadEditor() }

  private val highlightingAttributes by lazy {
    EditorColorsManager.getInstance()
      .globalScheme
      .getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
  }

  private val jwt = Jwt(configuration, encodedText, headerText, payloadText)

  // -- Initialization ------------------------------------------------------ //

  init {
    liveConversion.afterChange(parentDisposable) { handleLiveConversionSwitch() }

    jwt.signature.secretEncodingMode.afterChangeConsumeEvent(null) { e ->
      if (e.valueChanged()) {
        convertFromUi(SIGNATURE_CONFIGURATION)
      }
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        cell(
            Splitter(true, 0.2f).apply {
              firstComponent = createEncodedComponent()
              secondComponent = createEncodingDecodingComponent()
            }
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  private fun createEncodedComponent(): JComponent = panel {
    row {
        cell(encodedEditor.component)
          .validationOnApply(encodedEditor.bindValidator(jwt.encodedErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
      .bottomGap(BottomGap.NONE)
    val signatureErrors = jwt.signatureErrorHolder.asComponentPredicate()
    row {
        text("<icon src='AllIcons.General.InspectionsOK'>&nbsp;Valid signature")
          .visibleIf(signatureErrors.not())
          .resizableColumn()
        text("")
          .bindText(jwt.signatureErrorHolder.asPropertyForTextCell())
          .visibleIf(signatureErrors)
          .resizableColumn()
      }
      .topGap(TopGap.NONE)
  }

  @Suppress("UnstableApiUsage")
  private fun createEncodingDecodingComponent(): JComponent = panel {
    row {
      val liveConversionCheckBox =
        checkBox("Live conversion").bindSelected(liveConversion).gap(RightGap.SMALL)

      button("▼ Decode") { convert(ENCODED) }
        .enabledIf(liveConversionCheckBox.selected.not())
        .gap(RightGap.SMALL)
      button("▲ Encode") {
          // This will set the signature algorithm in the header and will run the encoding.
          convert(SIGNATURE_CONFIGURATION)
        }
        .enabledIf(liveConversionCheckBox.selected.not())
    }

    if (context.prioritizeVerticalLayout) {
      row {
          cell(
              Splitter(true, 0.3f).apply {
                firstComponent = createHeaderEditorComponent()
                secondComponent = createPayloadEditorComponent()
              }
            )
            .align(Align.FILL)
            .resizableColumn()
        }
        .resizableRow()
        .bottomGap(BottomGap.NONE)
    } else {
      row {
          cell(
              Splitter(false, 0.5f).apply {
                firstComponent = createHeaderEditorComponent()
                secondComponent = createPayloadEditorComponent()
              }
            )
            .align(Align.FILL)
            .resizableColumn()
        }
        .resizableRow()
        .bottomGap(BottomGap.NONE)
    }

    collapsibleGroup("签名算法配置") {
        lateinit var signatureAlgorithmComboBox: ComboBox<SignatureAlgorithm>
        row {
            signatureAlgorithmComboBox =
              comboBox(SignatureAlgorithm.entries)
                .label("算法:")
                .bindItem(jwt.signature.algorithm)
                .whenItemSelectedFromUi { convertFromUi(SIGNATURE_CONFIGURATION) }
                .component
          }
          .layout(RowLayout.PARENT_GRID)
          .topGap(TopGap.NONE)

        row {
            // Bug: The label from `expandableTextField().label(...)` disappears
            // if the encoding selection gets changed
            label("密钥(Secret key):")
            expandableTextField()
              .align(AlignX.FILL)
              .bindText(jwt.signature.secret)
              .whenTextChangedFromUi { convertFromUi(SIGNATURE_CONFIGURATION) }
              .gap(RightGap.SMALL)
              .resizableColumn()
              .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }

            val encodingActions =
              mutableListOf<AnAction>().apply {
                SecretKeyEncodingMode.entries.forEach { secretKeyEncodingModeValue ->
                  add(
                    SimpleToggleAction(
                      text = secretKeyEncodingModeValue.title,
                      icon = AllIcons.Actions.ToggleSoftWrap,
                      isSelected = {
                        jwt.signature.secretEncodingMode.get() == secretKeyEncodingModeValue
                      },
                      setSelected = {
                        jwt.signature.secretEncodingMode.set(secretKeyEncodingModeValue)
                      },
                    )
                  )
                }
              }
            actionButton(
              UiUtils.actionsPopup(
                title = "编码",
                icon = AllIcons.General.Settings,
                actions = encodingActions,
              )
            )
          }
          .visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind?.keyFactory == null })
          .layout(RowLayout.PARENT_GRID)

        row {
            textArea()
              .rows(5)
              .align(Align.FILL)
              .label(label = "私钥（Private key）:", position = LabelPosition.TOP)
              .bindText(jwt.signature.privateKey)
              .setValidationResultBorder()
              .whenTextChangedFromUi { convertFromUi(SIGNATURE_CONFIGURATION) }
              .validationInfo(jwt.signature.privateKeyErrorHolder.asValidation())
              .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }
          }
          .visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind?.keyFactory != null })

        row {
          checkBox("严格的密钥需求验证")
            .bindSelected(jwt.signature.strictSigningKeyValidation)
            .whenStateChangedFromUi { convertFromUi(SIGNATURE_CONFIGURATION) }
            .gap(RightGap.SMALL)
          contextHelp(
            "JSON Web 算法 （JWA） 的 RFC 7518 指定了密钥或密钥在计算签名时应满足的一些限制（例如，最小长度）。此选项可用于强制实施这些限制。"
          )
        }
      }
      .apply { expanded = false }
      .topGap(TopGap.NONE)
  }

  private fun createPayloadEditorComponent(): JComponent = panel {
    row {
        cell(payloadEditor.component)
          .validationOnApply(payloadEditor.bindValidator(jwt.payloadErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  private fun createHeaderEditorComponent(): JComponent = panel {
    row {
        cell(headerEditor.component)
          .validationOnApply(headerEditor.bindValidator(jwt.headerErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  override fun afterBuildUi() {
    convertFromUi(ENCODED)
  }

  override fun reset() {
    convert(ENCODED)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun convertFromUi(changeOrigin: ChangeOrigin) {
    if (!liveConversion.get()) {
      return
    }

    convert(changeOrigin)
  }

  private fun convert(changeOrigin: ChangeOrigin) {
    if (configuration.isResetting) {
      return
    }

    if (!isDisposed && !conversionAlarm.isDisposed) {
      conversionAlarm.cancelAllRequests()
      conversionAlarm.addRequest({ doConvert(changeOrigin) }, 100)
    }
  }

  private fun doConvert(changeOrigin: ChangeOrigin) {
    when (changeOrigin) {
      ENCODED -> jwt.decodeJwt()
      HEADER_OR_PAYLOAD -> jwt.encodeJwt()
      SIGNATURE_CONFIGURATION -> {
        jwt.setAlgorithmInHeader()
        jwt.encodeJwt()
      }
    }

    highlightDotSeparator()
    highlightHeaderClaims()
    highlightPayloadClaims()

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun highlightDotSeparator() {
    val highlightDotSeparator = {
      encodedEditor.removeTextRangeHighlighters(ENCODED_DOT_SEPARATOR_GROUP_ID)
      val encoded = encodedText.get()
      var dotIndex = encoded.indexOf('.')
      var i = 0
      while (dotIndex != -1) {
        encodedEditor.highlightTextRange(
          TextRange(dotIndex, dotIndex + 1),
          ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER,
          highlightingAttributes,
          ENCODED_DOT_SEPARATOR_GROUP_ID,
        )
        dotIndex = encoded.indexOf('.', dotIndex + 1)
        i++
      }
    }
    if (!isDisposed && !highlightEncodedAlarm.isDisposed) {
      highlightEncodedAlarm.cancelAllRequests()
      highlightEncodedAlarm.addRequest(highlightDotSeparator, 100)
    }
  }

  private fun highlightHeaderClaims() {
    if (!isDisposed && !highlightHeaderAlarm.isDisposed) {
      highlightHeaderAlarm.cancelAllRequests()
      highlightHeaderAlarm.addRequest({ doHighlightClaims(headerEditor) }, 100)
    }
  }

  private fun highlightPayloadClaims() {
    if (!isDisposed && !highlightPayloadAlarm.isDisposed) {
      highlightPayloadAlarm.cancelAllRequests()
      highlightPayloadAlarm.addRequest({ doHighlightClaims(payloadEditor) }, 100)
    }
  }

  private fun doHighlightClaims(editor: AdvancedEditor) {
    editor.removeTextRangeHighlighters(HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID)

    UNIX_TIMESTAMP_SECONDS_JSON_VALUE_REGEX.findAll(editor.text).forEach {
      val unixTimestampSecondsMatch = it.groups[1]
      if (unixTimestampSecondsMatch != null) {
        val textRange =
          TextRange(unixTimestampSecondsMatch.range.first, unixTimestampSecondsMatch.range.last + 1)
        editor.highlightTextRange(
          textRange,
          UNIX_TIMESTAMP_HIGHLIGHT_LAYER,
          null,
          HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID,
          ClaimFeatureUnixTimestampGutterIconRenderer(
            textRange,
            unixTimestampSecondsMatch.value.toLong(),
          ),
        )
      }
    }

    CLAIM_REGEX.findAll(editor.text)
      .mapNotNull {
        val claimMatch = it.groups[1]
        if (claimMatch != null) {
          val standardClaim = StandardClaim.findByFieldName(claimMatch.value)
          if (standardClaim != null) {
            return@mapNotNull claimMatch.range to standardClaim
          }
        }
        null
      }
      .forEach { (claimRange, standardClaim) ->
        val textRange = TextRange(claimRange.first, claimRange.last + 1)
        editor.highlightTextRange(
          textRange,
          CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER,
          null,
          HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID,
          StandardClaimGutterIconRenderer(textRange, standardClaim),
        )
      }
  }

  private fun createEncodedEditor(): AdvancedEditor =
    createEditor(
      id = "encoded",
      changeOrigin = ENCODED,
      title = "编码",
      language = PlainTextLanguage.INSTANCE,
      textProperty = encodedText,
    ) {
      highlightDotSeparator()
    }

  private fun createHeaderEditor(): AdvancedEditor =
    createEditor(
      id = "header",
      changeOrigin = HEADER_OR_PAYLOAD,
      title = "请求头",
      language = JsonLanguage.INSTANCE,
      textProperty = headerText,
    ) {
      highlightHeaderClaims()
    }

  private fun createPayloadEditor(): AdvancedEditor =
    createEditor(
      id = "payload",
      changeOrigin = HEADER_OR_PAYLOAD,
      title = "负载",
      language = JsonLanguage.INSTANCE,
      textProperty = payloadText,
    ) {
      highlightPayloadClaims()
    }

  private fun createEditor(
    id: String,
    changeOrigin: ChangeOrigin,
    title: String,
    language: Language,
    textProperty: ValueProperty<String>,
    onTextChangeFromUi: (() -> Unit)? = null,
  ) =
    AdvancedEditor(
        id = id,
        context = context,
        configuration = configuration,
        project = project,
        title = title,
        editorMode = EditorMode.INPUT_OUTPUT,
        parentDisposable = parentDisposable,
        textProperty = textProperty,
        initialLanguage = language,
      )
      .apply {
        onFocusGained { lastActiveInput = this }
        this.onTextChangeFromUi { _ ->
          lastActiveInput = this
          convertFromUi(changeOrigin)
          onTextChangeFromUi?.invoke()
        }
      }

  private fun handleLiveConversionSwitch() {
    if (liveConversion.get()) {
      // Trigger a text change. So if the text was changed in manual mode, it
      // will now be encoded/decoded once during the switch to live mode.
      when (lastActiveInput) {
        encodedEditor -> convert(ENCODED)
        headerEditor,
        payloadEditor -> {
          // This will set the signature algorithm in the header and will run the encoding.
          convert(SIGNATURE_CONFIGURATION)
        }

        null -> {}
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class StandardClaimGutterIconRenderer(
    private val textRange: TextRange,
    private val standardClaim: StandardClaim,
  ) : GutterIconRenderer(), DumbAware {

    override fun getTooltipText(): String = standardClaim.toString()

    override fun getIcon(): Icon = AllIcons.Gutter.JavadocRead

    override fun equals(other: Any?): Boolean {
      return if (other != null && other is StandardClaimGutterIconRenderer) {
        other.textRange == textRange && other.standardClaim == standardClaim
      } else {
        false
      }
    }

    override fun hashCode(): Int = Objects.hash(textRange, standardClaim)

    override fun getAlignment(): Alignment = Alignment.RIGHT
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ClaimFeatureUnixTimestampGutterIconRenderer(
    private val textRange: TextRange,
    private val unixTimestampSeconds: Long,
  ) : GutterIconRenderer(), DumbAware {

    override fun getTooltipText(): String {
      val tooltipText = StringJoiner("<br /><br />")

      tooltipText.add(
        Instant.ofEpochSecond(unixTimestampSeconds)
          .atZone(ZoneId.systemDefault())
          .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
      )

      val diff =
        DateFormatUtil.formatBetweenDates(
          unixTimestampSeconds.times(1000),
          System.currentTimeMillis(),
        )
      tooltipText.add("${diff.capitalize()}.")

      return tooltipText.toString()
    }

    override fun getIcon(): Icon = clockGutterIcon

    override fun equals(other: Any?): Boolean {
      return if (other != null && other is ClaimFeatureUnixTimestampGutterIconRenderer) {
        other.textRange == textRange && other.unixTimestampSeconds == unixTimestampSeconds
      } else {
        false
      }
    }

    override fun hashCode(): Int = Objects.hash(textRange, unixTimestampSeconds)

    override fun getAlignment(): Alignment = Alignment.LEFT

    companion object {

      private val clockGutterIcon =
        IconManager.getInstance()
          .getIcon(
            "dev/turingcomplete/intellijdevelopertoolsplugin/icons/clock_gutter.svg",
            JwtEncoderDecoder::class.java.classLoader,
          )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class ChangeOrigin {

    ENCODED,
    HEADER_OR_PAYLOAD,
    SIGNATURE_CONFIGURATION,
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class SignatureAlgorithmKind(val keyFactory: KeyFactory?) {

    HMAC(null),
    RSA(KeyFactory.getInstance("RSA")),
    ECDSA(KeyFactory.getInstance("EC")),
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class SignatureAlgorithm(
    val jwtHeaderValue: String,
    val kind: SignatureAlgorithmKind,
    @Suppress("unused") // May be used for JWK validation
    val algorithmIdentifiers: String,
  ) {

    HMAC256("HS256", HMAC, HMAC_SHA256),
    HMAC384("HS384", HMAC, HMAC_SHA384),
    HMAC512("HS512", HMAC, HMAC_SHA512),
    RSA256("RS256", RSA, RSA_USING_SHA256),
    RSA384("RS384", RSA, RSA_USING_SHA384),
    RSA512("RS512", RSA, RSA_USING_SHA512),
    ECDSA256("ES256", ECDSA, ECDSA_USING_P256_CURVE_AND_SHA256),
    ECDSA384("ES384", ECDSA, ECDSA_USING_P384_CURVE_AND_SHA384),
    ECDSA512("ES512", ECDSA, ECDSA_USING_P521_CURVE_AND_SHA512);

    override fun toString(): String = "$name ($jwtHeaderValue)"

    companion object {

      fun findByJwtHeaderValue(jwtHeaderValue: String): SignatureAlgorithm? =
        entries.firstOrNull { it.jwtHeaderValue == jwtHeaderValue }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class Jwt(
    configuration: DeveloperToolConfiguration,
    val encoded: ValueProperty<String>,
    val header: ValueProperty<String>,
    val payload: ValueProperty<String>,
  ) {

    val encodedErrorHolder = ErrorHolder()
    val headerErrorHolder = ErrorHolder()
    val payloadErrorHolder = ErrorHolder()
    val signatureErrorHolder =
      ErrorHolder(addErrorIconToMessage = true, surroundMessageWithHtml = false)

    val signature = Signature(configuration, signatureErrorHolder)

    fun decodeJwt() {
      clearErrorHolders()

      val jwtParts = encoded.get().split(".")
      val numOfJwtParts = jwtParts.size

      // Header
      if (numOfJwtParts >= 1) {
        val handleError: (Exception) -> Unit = { error ->
          header.set(jwtParts[0])
          headerErrorHolder.add(error)
        }
        parseAsJson(text = jwtParts[0], textIsBase64 = true, handleError) {
          parseHeader(it)
          header.set(ObjectMapperService.instance.prettyPrintJson(it))
        }
      } else {
        header.set("")
      }

      // Payload
      if (numOfJwtParts >= 2) {
        val handleError: (Exception) -> Unit = { error ->
          payload.set(jwtParts[1])
          payloadErrorHolder.add(error)
        }
        parseAsJson(text = jwtParts[1], textIsBase64 = true, handleError) {
          payload.set(ObjectMapperService.instance.prettyPrintJson(it))
        }
      } else {
        payload.set("")
      }

      // Signature
      if (numOfJwtParts >= 3 && headerErrorHolder.isNotSet()) {
        signature.compute(jwtParts[0], jwtParts[1])?.let { expectedSignature ->
          val actualSignature = jwtParts[2]
          if (expectedSignature != actualSignature) {
            signatureErrorHolder.add(
              "签名无效。检查 'Signature Algorithm Configuration' 部分中的配置。"
            )
          }
        }
      } else {
        signatureErrorHolder.add("编码的 JWT 没有签名部分")
      }
    }

    fun encodeJwt() {
      clearErrorHolders()

      val jsonMapper = ObjectMapperService.instance.jsonMapper()

      val headerJson =
        try {
          val headerJson = jsonMapper.readTree(header.get())
          parseHeader(headerJson)
          headerJson
        } catch (e: Exception) {
          headerErrorHolder.add(e)
          null
        }

      val payloadJson =
        try {
          jsonMapper.readTree(payload.get())
        } catch (e: Exception) {
          payloadErrorHolder.add(e)
          null
        }

      if (headerErrorHolder.isSet() || payloadErrorHolder.isSet()) {
        encoded.set("")
        signatureErrorHolder.add("由于标头或有效负载错误，无法计算签名")
      } else {
        val encodedHeader =
          urlEncoder
            .encode(jsonMapper.writeValueAsString(headerJson!!).encodeToByteArray())
            .decodeToString()
        val encodedPayload =
          urlEncoder
            .encode(jsonMapper.writeValueAsString(payloadJson!!).encodeToByteArray())
            .decodeToString()
        val encodedSignature = signature.compute(encodedHeader, encodedPayload)
        if (encodedSignature == null) {
          encoded.set("")
          signatureErrorHolder.addIfNoErrors(
            "由于签名配置错误，无法计算签名"
          )
        } else {
          encoded.set("${encodedHeader}.${encodedPayload}.$encodedSignature")
        }
      }
    }

    fun setAlgorithmInHeader() {
      parseAsJson(text = header.get(), textIsBase64 = false, { headerErrorHolder.add(it) }) {
        headerNode ->
        if (headerNode is ObjectNode) {
          headerNode.put("alg", signature.algorithm.get().jwtHeaderValue)
          header.set(ObjectMapperService.instance.prettyPrintJson(headerNode))
        }
      }
    }

    private fun clearErrorHolders() {
      encodedErrorHolder.clear()
      headerErrorHolder.clear()
      payloadErrorHolder.clear()
      signatureErrorHolder.clear()
    }

    private fun parseHeader(headerNode: JsonNode) {
      if (headerNode.has("alg")) {
        val algFieldValue = headerNode.get("alg").asText()
        val algorithm = SignatureAlgorithm.findByJwtHeaderValue(algFieldValue)
        if (algorithm != null) {
          signature.algorithm.set(algorithm)
        } else {
          headerErrorHolder.add("不支持的算法： '$algFieldValue'")
        }
      } else {
        headerErrorHolder.add("缺少算法标头字段: 'alg'")
      }
    }

    private fun parseAsJson(
      text: String,
      textIsBase64: Boolean,
      handleError: (Exception) -> Unit,
      handleResult: (JsonNode) -> Unit,
    ) {
      try {
        val actualText = if (textIsBase64) text.decodeBase64String() else text
        val jsonNode = ObjectMapperService.instance.jsonMapper().readTree(actualText)
        handleResult(jsonNode)
      } catch (e: Exception) {
        handleError(e)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class Signature(
    configuration: DeveloperToolConfiguration,
    private val signatureErrorHolder: ErrorHolder,
  ) {

    val algorithm = configuration.register("algorithm", DEFAULT_SIGNATURE_ALGORITHM)
    val strictSigningKeyValidation =
      configuration.register("signingKeyValidation", SIGNING_KEY_VALIDATION_DEFAULT, CONFIGURATION)
    val secret = configuration.register("secret", "", SENSITIVE, EXAMPLE_SECRET)
    val privateKey =
      configuration.registerWithExampleProvider("privateKey", "", SENSITIVE) {
        if (algorithm.get().kind == RSA) EXAMPLE_RSA_PRIVATE_KEY else EXAMPLE_EC_PRIVATE_KEY
      }
    val secretEncodingMode = configuration.register("secretKeyEncodingMode", RAW, CONFIGURATION)

    val privateKeyErrorHolder = ErrorHolder()

    init {
      handleAlgorithmChange()
      algorithm.afterChangeConsumeEvent(null) { e ->
        if (e.valueChanged()) {
          handleAlgorithmChange()
        }
      }
    }

    fun compute(encodedHeader: String, encodedPayload: String): String? {
      privateKeyErrorHolder.clear()

      return try {
        val signingKey = createSigningKey() ?: return null
        ExtendedJsonWebSignature()
          .apply {
            // The algorithm gets derivative from the header property `alg`
            setEncodedHeader(encodedHeader)
            setEncodedPayload(encodedPayload)
            setKey(signingKey)
            isDoKeyValidation = strictSigningKeyValidation.get()
            sign()
          }
          .encodedSignature
      } catch (e: Exception) {
        signatureErrorHolder.add("无法计算签名:", ExceptionUtil.getRootCause(e))
        null
      }
    }

    private fun createSigningKey(): Key? {
      val signatureAlgorithm = algorithm.get()
      return when (signatureAlgorithm.kind) {
        HMAC ->
          HmacKey(
            when (secretEncodingMode.get()) {
              RAW -> secret.get().encodeToByteArray()
              BASE32 -> Base32().decode(secret.get())
              BASE64 -> Base64.getDecoder().decode(secret.get())
            }
          )

        RSA,
        ECDSA -> readPrivateKey(signatureAlgorithm.kind.keyFactory!!) ?: return null
      }
    }

    private fun readPrivateKey(keyFactory: KeyFactory) =
      try {
        val privateKeyValue = privateKey.get()
        if (privateKeyValue.isBlank()) {
          privateKeyErrorHolder.add("必须提供私钥")
          null
        } else {
          keyFactory.generatePrivate(PKCS8EncodedKeySpec(toRawKey(privateKey.get())))
        }
      } catch (e: Exception) {
        privateKeyErrorHolder.add(e)
        null
      }

    private fun toRawKey(keyInput: String): ByteArray =
      Base64.getDecoder().decode(keyInput.replace(RAW_KEY_REGEX, ""))

    private fun handleAlgorithmChange() {
      if (generalSettings.loadExamples.get()) {
        loadExampleSecrets()
      }
    }

    private fun loadExampleSecrets() {
      val privateKeyValue = privateKey.get()
      when (algorithm.get().kind) {
        HMAC -> {
          if (secret.get().isBlank()) {
            secret.set(EXAMPLE_SECRET)
          }
        }

        RSA -> {
          if (privateKeyValue.isBlank() || privateKeyValue == EXAMPLE_EC_PRIVATE_KEY) {
            privateKey.set(EXAMPLE_RSA_PRIVATE_KEY)
          }
        }

        ECDSA -> {
          if (privateKeyValue.isBlank() || privateKeyValue == EXAMPLE_RSA_PRIVATE_KEY) {
            privateKey.set(EXAMPLE_EC_PRIVATE_KEY)
          }
        }
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class StandardClaim(
    val fieldName: String,
    val title: String,
    val description: String,
  ) {

    TYP(fieldName = "typ", title = "类型", description = "表示此 Token 为 JWT。"),
    ISSUER(fieldName = "iss", title = "发行商", description = "JWT 的发行者。"),
    SUBJECT(fieldName = "sub", title = "主体", description = "JWT 的主体。"),
    AUDIENCE(fieldName = "aud", title = "接收者", description = "JWT 的接收方。"),
    EXPIRATION_TIME(
      fieldName = "exp",
      title = "过期时间",
      description = "JWT 过期时间。",
    ),
    NOT_BEFORE_TIME(
      fieldName = "nbf",
      title = "不早于时间",
      description = "JWT 在此时间之后有效。",
    ),
    ISSUED_AT_TIME(
      fieldName = "iat",
      title = "发布时间",
      description = "JWT 此时发布。",
    ),
    JWT_ID(fieldName = "jti", title = "JWT ID", description = "JWT 的唯一标识符。"),
    ALG(
      fieldName = "alg",
      title = "算法",
      description = "计算此 JWT 签名的算法。",
    ),
    AZP(
      fieldName = "azp",
      title = "被授权方",
      description = "向其颁发 JWT 的一方。",
    ),
    SID(fieldName = "sid", title = "Session ID", description = "An unique session ID."),
    NONCE(
      fieldName = "nonce",
      title = "Nonce",
      description = "用于将客户端会话与此 JWT 关联的值。",
    ),
    AT_HASH(
      fieldName = "at_hash",
      title = "访问令牌哈希值",
      description = "访问令牌的哈希值。",
    ),
    C_HASH(fieldName = "c_hash", title = "代码哈希值", description = "代码的哈希值。"),
    ACT(fieldName = "act", title = "Actor", description = "委托或代理场景中的原始主体"),
    AUTH_TIME(
      fieldName = "auth_time",
      title = "鉴权时间",
      description = "用户身份验证的时间。",
    ),
    SCOPE(fieldName = "scope", title = "范围", description = "授予令牌的权限.");

    override fun toString(): String = "<html>$title ($fieldName)<br/>$description</html>"

    companion object {

      fun findByFieldName(fieldName: String): StandardClaim? =
        entries.firstOrNull { it.fieldName == fieldName }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<JwtEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = "JSON Web 令牌 （JWT）",
        contentTitle = "JSON Web 令牌 （JWT） 解码器/编码器",
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> JwtEncoderDecoder) = { configuration ->
      JwtEncoderDecoder(context, configuration, parentDisposable, project)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  // -- Inner Type ---------------------------------------------------------- //

  private class ExtendedJsonWebSignature : JsonWebSignature() {

    @Suppress("RedundantVisibilityModifier") // False-positive
    public override fun setEncodedHeader(encodedHeader: String?) {
      super.setEncodedHeader(encodedHeader)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class SecretKeyEncodingMode(val title: String) {

    RAW("Raw"),
    BASE32("Base32 Encoded"),
    BASE64("Base64 Encoded"),
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

    private val UNIX_TIMESTAMP_SECONDS_JSON_VALUE_REGEX =
      Regex(":\\s*(?<unixTimestampSeconds>\\b\\d{1,10}\\b)")
    private val CLAIM_REGEX = Regex("\\s?\"(?<name>[a-zA-Z]+)\"\\s?:")
    private const val UNIX_TIMESTAMP_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1
    private const val CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER = UNIX_TIMESTAMP_HIGHLIGHT_LAYER - 1

    private const val HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID = "claims"
    private const val ENCODED_DOT_SEPARATOR_GROUP_ID = "encodedDotSeparator"
    private const val ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER = HighlighterLayer.SELECTION - 1

    private val RAW_KEY_REGEX = Regex("\\r?\\n|\\r|\\s?-+(BEGIN|END).*KEY-+\\s?")

    private val DEFAULT_SIGNATURE_ALGORITHM = HMAC256
    private const val EXAMPLE_ENCODED =
      "ewogICJ0eXAiOiJKV1QiLAogICJhbGciOiJIUzI1NiIKfQ.ewogICJqdGkiOiI5NjQ5MmQ1OS0wYWQ1LTRjMDAtODkyZC01OTBhZDVhYzAwZjMiLAogICJzdWIiOiIwMTIzNDU2Nzg5IiwKICAibmFtZSI6IkpvaG4gRG9lIiwKICAiaWF0IjoxNjgxMDQwNTE1Cn0.IqeNl3lHSUfPfEYmttvlQp1sH9LpAoPJlUiSv4XPDSE"
    private const val EXAMPLE_SECRET = "s3cre!"
    private val EXAMPLE_HEADER =
      """
          {
            "typ":"JWT",
            "alg":"HS256"
          }
          """
        .trimIndent()
    private val EXAMPLE_PAYLOAD =
      """
          {
            "jti":"96492d59-0ad5-4c00-892d-590ad5ac00f3",
            "sub":"0123456789",
            "name":"John Doe",
            "iat":1681040515
          }
          """
        .trimIndent()

    private const val SIGNING_KEY_VALIDATION_DEFAULT = false

    private val EXAMPLE_RSA_PRIVATE_KEY =
      """
-----BEGIN RSA PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDdadLFj3DqaYtpZ1ik6ejpIIAU
2KhFqygTvR6SSS9RmcFQu/vojHWzQUhm8aqrGYVkDXCHvEcyBPcZUlWBczcDwQ5YF8VktRpMxfAI
K/OZRmfrhK9jAZsxOPCCXMOY+JoCbEqEOpsClbHKbgNBgw4AfsISzuWODa47KucIQad202lUZMQ5
iBQ9CRcSfSis6HyvCMTY5li/9a+O78FfqIGUE4FHeJpsiay2z2AMEzwBPoURkTaSjjOT25e+GY7k
ntilnVne1ORdOPMnOcd28COex55Z+C4QlOr2UIDaAinTAG/0ozwWxd8OaVJJy3mj3dd3AeD2vBMm
ycnhrM+sccqHAgMBAAECggEARelztZDg2QuhixMoUM5RDkeGWc69d14fZfgpzowQRmZTvZ/V32x2
f7bl2yeEucjxrxF1Tk67dkZOFa9DM4BDR0qusk8zM2Th3IsFizcBkIzEJIA9dvgbXjP58VfEJSme
S5SRBOaSaoME5APPwGBWy/46XoD4x912/dTCpX9Blwl81i7EO8o3NnYhsCWeVoUJTWBzN95OchZF
ozV4pFgv0tZqTNa7VhJtWHHiKkCpdK7gA9SeVEqEeL1TADAa2ngy3BIRfTgdAct6/4N+ZlVsaIXB
1Gnw2RaOoUbHy1PCA6ygtH5lz65p0JdWGcO5l+JNeYmOIeOdJ3QWbVI+CikPGQKBgQDv/JrTewDt
BK5KBpotFsrJeDFKOkC6A8aNeGliAEgJYvCk7zb8RtKCx7ViaYGYWJYj30oYejYEE+vFT7sgzXfE
JPAEiMw4uKeIrbX8QEIP+R25S8iRr657DkTxOvyhO2oQcC7UkZagvrVyQ17VgjtjxGWbc5bRBk5v
u1ZV9VMZuQKBgQDsL/PsLCX3YRBB5+0rpWoTKKrmFtGh31oue+d37Nd7oxBzb2uyF4Q29+zoy1on
EnHNamjjdR95NZoOjEsIIKDTV1C/bsS7be53m0mwKQfecKIXJ+7VN4UsYZXjCajHCr3NFHiIU8ct
pcKGtg7ga5cERIBtrPAi9Qzi7/o1MxUmPwKBgFuSaMWPZuAJ7DNE56mSy9gqa6xmI/KWpDmxG40Q
jGxAe5CD0thacdMDPzwJBDFMhCW1+wDyCRBvRYSpkr7GiA+pBIjGZh6ynwKxPgK9xjdwGB5vQ14L
yikcXcQqfOFM2YDiPYxQ7Ufy3St3d4VCx0SfWSIC7iZeIKnTsvLjxEzJAoGBAKcLFzou0z9N3+Cs
9pnK6OXZ+ly3QNZ6kF6V9VRlJtXjs0vhPsr7ROBXoq/WutEtg11j6AEPIg5o8adeY+bApN40QADU
h8GD84eWRZyYuF8DTDCSZqFYHhEQh6DGgR8dIrX7x2+ryRAozxbVhloE3g7/n9Fx4Xjn1ZBfZ5fe
pBOjAoGAcw2M22BK3NWOHhJ8EC4p6aUIR96lNcCWE/ij+MWCcRdotLDSDuT1q13C+UTxDZ5PsmDs
N/bhCDRZYZoLYo0/h6v4zKBDaX05nVUTCYux0Fo2HGrj5S0bjmgyRcr8+enA3CTzCHZPWZ7ZeADb
0Mbtt/Q4JyOCgwORgXJVQBHxxIQ=
-----END RSA PRIVATE KEY-----
    """
        .trimIndent()
    private val EXAMPLE_EC_PRIVATE_KEY =
      """
-----BEGIN EC PRIVATE KEY-----
MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDQ+B6qEzr/M2sql4X+09X9YlYt8BKA
HX8Q7/6s4KC3qQ==
-----END RSA PRIVATE KEY-----
    """
        .trimIndent()
  }
}
