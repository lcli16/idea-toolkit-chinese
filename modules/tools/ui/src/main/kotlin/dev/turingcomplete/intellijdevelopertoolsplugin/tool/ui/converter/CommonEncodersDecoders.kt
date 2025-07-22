package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeFromAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.encodeToAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate.Companion.createPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.BarcodeGenerator.Format
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.JsonSchemaValidator.ValidationState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.commons.codec.binary.Base32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// -- Properties ---------------------------------------------------------- //
// -- Exported Methods ---------------------------------------------------- //
// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //

class Base32EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("base32-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = Base32().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray = Base32().decode(target)

  class Factory : DeveloperUiToolFactory<Base32EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("base32-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("base32-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("base32-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Base32EncoderDecoder) = { configuration ->
      Base32EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class Base64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = Base64.getEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray = Base64.getDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<Base64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Base64EncoderDecoder) = { configuration ->
      Base64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class UrlBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("url-base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    Base64.getUrlEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray =
    Base64.getUrlDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<UrlBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("url-base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("url-base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("url-base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> UrlBase64EncoderDecoder) = { configuration ->
      UrlBase64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class MimeBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("mime-base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    Base64.getMimeEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray =
    Base64.getMimeDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<MimeBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("mime-base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("mime-base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("mime-base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> MimeBase64EncoderDecoder) = { configuration ->
      MimeBase64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class AsciiEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("ascii-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = source.encodeToAscii()

  override fun doConvertToSource(target: ByteArray): ByteArray = target.decodeFromAscii()

  class Factory : DeveloperUiToolFactory<AsciiEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("ascii-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("ascii-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("ascii-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> AsciiEncoderDecoder) = { configuration ->
      AsciiEncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class UrlEncodingEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("url-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    URLEncoder.encode(String(source), StandardCharsets.UTF_8.name()).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    URLDecoder.decode(String(target), StandardCharsets.UTF_8.name()).toByteArray()

  class Factory : DeveloperUiToolFactory<UrlEncodingEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("url-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("url-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("url-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> UrlEncodingEncoderDecoder) = { configuration ->
      UrlEncodingEncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}
// -- Properties ---------------------------------------------------------- //
// -- Exported Methods ---------------------------------------------------- //
// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //


class AesEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) : EncoderDecoder(
  configuration = configuration,
  parentDisposable = parentDisposable,
  context = context,
  project = project,
  title = UiToolsBundle.message("aes-encoding.title"),
) {
  private fun validateKeyLength(key: ByteArray, expectedLength: Int): ByteArray {
    return when {
      key.size == expectedLength -> key
      key.size < expectedLength -> {
        val paddedKey = ByteArray(expectedLength)
        System.arraycopy(key, 0, paddedKey, 0, key.size)
        paddedKey
      }
      else -> key.copyOfRange(0, expectedLength)
    }
  }
  private var aesModeList = configuration.register("aseMode", AesMode.ECB)
  private enum class AesMode(val title: String,
                             val id: String,
                             val objectMapper: (ObjectMapperService) -> ObjectMapper,) {
    ECB("ECB", "AES/ECB", { it.jsonMapper() }),
    CBC("CBC", "AES/CBC", { it.jsonMapper() }),
    CTR("CTR", "AES/CTR", { it.jsonMapper() }),
    OFB("OFB", "AES/OFB", { it.jsonMapper() }),
    OFB8("OFB8", "AES/CFB8", { it.jsonMapper() }),
    CFB("CFB", "AES/CFB", { it.jsonMapper() }),
    CFB8("CFB8", "AES/CFB8", { it.jsonMapper() }),
    CFB128("CFB128", "AES/CFB128",{ it.jsonMapper() });


    override fun toString(): String = title

    fun parse(text: ByteArray): JsonNode = objectMapper(ObjectMapperService.instance).readTree(text)

    fun writeAsBytes(root: JsonNode): ByteArray =
      objectMapper(ObjectMapperService.instance).writeValueAsBytes(root)
  }

  private var fillModeEnum = configuration.register("aesFillMode", FillMode.PKCS7)
  private enum class FillMode(val title: String,
                             val id: String,
                             val objectMapper: (ObjectMapperService) -> ObjectMapper,) {
    PKCS7("PKCS7", "PKCS7Padding", { it.jsonMapper() }),
    Zero("Zeros", "NoPadding", { it.jsonMapper() }),
    ISO("ISO10126", "ISO10126Padding", { it.jsonMapper() }),
    ANSIX("ANSIX923", "ANSIX923Padding", { it.jsonMapper() }),
    NONE("None", "NoPadding", { it.jsonMapper() });

    override fun toString(): String = title

    fun parse(text: ByteArray): JsonNode = objectMapper(ObjectMapperService.instance).readTree(text)

    fun writeAsBytes(root: JsonNode): ByteArray =
      objectMapper(ObjectMapperService.instance).writeValueAsBytes(root)
  }

  private var keyLengthEnum = configuration.register("keyLength", KeyLength.BITS128)
  private enum class KeyLength(val title: String,
                              val id: String,
                              val objectMapper: (ObjectMapperService) -> ObjectMapper,) {
    BITS128("128bits", "128bits", { it.jsonMapper() }),
    BITS192("192bits", "192bits", { it.jsonMapper() }),
    BITS256("256bits", "256bits", { it.jsonMapper() });

    override fun toString(): String = title

    fun parse(text: ByteArray): JsonNode = objectMapper(ObjectMapperService.instance).readTree(text)

    fun writeAsBytes(root: JsonNode): ByteArray =
      objectMapper(ObjectMapperService.instance).writeValueAsBytes(root)
  }
  private var pwdKey = configuration.register("key", "")
  private var iv = configuration.register("iv", "")
  private fun AesMode.requiresIv(): Boolean {
    return when (this) {
      AesMode.ECB -> false
      else -> true
    }
  }
  private var output = configuration.register("output", "base64")

  override fun Panel.buildSourceTopConfigurationUi() {
    lateinit var aesModeComboBox: ComboBox<AesMode>
    // 加密模式
    row {
      aesModeComboBox = comboBox(AesMode.entries)
        .label(UiToolsBundle.message("aes-encoding.mode-title"))

        .bindItem(aesModeList).component
    }
    // 填充模式
    row {
      comboBox(FillMode.entries)
        .label(UiToolsBundle.message("aes-encoding.fill-mode-title"))
        .bindItem(fillModeEnum)
    }
    // 密钥长度
    row {
      comboBox(KeyLength.entries)
        .label(UiToolsBundle.message("aes-encoding.key-length-title"))
        .bindItem(keyLengthEnum)
    }
    // 密钥
    row {
      textField()
        .label(UiToolsBundle.message("aes-encoding.key-title"))
        .bindText(pwdKey)
    }
      .layout(RowLayout.PARENT_GRID)
    // IV 向量
    val isEcb = PropertyComponentPredicate(aesModeList, AesMode.ECB)

    row {
      textField()
        .label(UiToolsBundle.message("aes-encoding.iv-title"))
        .bindText(iv)
    }
      .layout(RowLayout.PARENT_GRID)
      .visibleIf(isEcb.not())

    row {
      comboBox(listOf("base64", "hex"))
        .label(UiToolsBundle.message("aes-encoding.output-title"))
        .bindItem(output)
    }
      .layout(RowLayout.PARENT_GRID)

   }
  object EncodingUtils {
    fun isBase64(input: String): Boolean {
      return try {
        Base64.getDecoder().decode(input)
        true
      } catch (e: IllegalArgumentException) {
        false
      }
    }

    fun isHex(input: String): Boolean {
      if (input.isEmpty() || input.length % 2 != 0) return false
      return input.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun hexToByteArray(input: String): ByteArray {
      val result = ByteArray(input.length / 2)
      for (i in input.indices step 2) {
        val hex = input.substring(i, i + 2)
        result[i / 2] = hex.toInt(16).toByte()
      }
      return result
    }
  }
  object PaddingUtils {
    fun zeroPad(data: ByteArray, blockSize: Int = 16): ByteArray {
      val paddingLength = blockSize - (data.size % blockSize)
      val padded = ByteArray(data.size + paddingLength)
      System.arraycopy(data, 0, padded, 0, data.size)
      return padded
    }

    fun unpadZero(data: ByteArray): ByteArray {
      var endIndex = data.size
      while (endIndex > 0 && data[endIndex - 1].toInt() == 0) {
        endIndex--
      }
      return data.copyOf(endIndex)
    }
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val fillMode = fillModeEnum.get()
    val expectedKeyLength = when (keyLengthEnum.get()) {
      KeyLength.BITS128 -> 16
      KeyLength.BITS192 -> 24
      KeyLength.BITS256 -> 32
    }

    val keyBytes = validateKeyLength(pwdKey.get().toByteArray(), expectedKeyLength)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val cipher = Cipher.getInstance("${aesModeList.get().id}/${fillMode.id}")


    // 根据加密模式判断是否需要 IV
    val mode = aesModeList.get()

    val cipherMode = if (mode.requiresIv()) {
      val ivBytes = iv.get().toByteArray()
      if (ivBytes.size != 16) throw IllegalArgumentException("IV 长度必须为 16 字节")
      val ivSpec = IvParameterSpec(ivBytes )
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    } else {
      cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    }
    if (source.isEmpty()){
      return source;
    }
    val paddedSource = if (fillMode == FillMode.Zero) PaddingUtils.zeroPad(source) else source
    val encrypted = cipher.doFinal(paddedSource)


    val result: ByteArray ;
    if (output.get() == "hex"){
        result =    encrypted.toHexString().toByteArray();
    } else{
       result =  Base64.getEncoder().encode(encrypted)
    }
    return result;
  }
  override fun doConvertToSource(target: ByteArray): ByteArray {
    val fillMode = fillModeEnum.get()
    val expectedKeyLength = when (keyLengthEnum.get()) {
      KeyLength.BITS128 -> 16
      KeyLength.BITS192 -> 24
      KeyLength.BITS256 -> 32
    }

    val keyBytes = validateKeyLength(pwdKey.get().toByteArray(), expectedKeyLength)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val cipher = Cipher.getInstance("${aesModeList.get().id}/${fillMode.id}")

    val mode = aesModeList.get()
    if (mode.requiresIv()) {
      val ivBytes = iv.get().toByteArray()
      if (ivBytes.size != 16) throw IllegalArgumentException("IV 长度必须为 16 字节")
      cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
    } else {
      cipher.init(Cipher.DECRYPT_MODE, keySpec)
    }

    val targetStr = String(target).trim()

    val decrypted: ByteArray;
    if (EncodingUtils.isHex(targetStr)){
        decrypted = cipher.doFinal(EncodingUtils.hexToByteArray(targetStr))
    }else if (EncodingUtils.isBase64(targetStr)){
      decrypted = cipher.doFinal(Base64.getDecoder().decode(targetStr))
    }  else{
      throw IllegalArgumentException("输入格式不支持，必须是 Base64 或 HEX 编码")
    }


    val result = if (fillMode == FillMode.Zero) PaddingUtils.unpadZero(decrypted) else decrypted

    return result
  }


  class Factory : DeveloperUiToolFactory<AesEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("aes-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("aes-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("aes-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> AesEncoderDecoder) = { configuration ->
      AesEncoderDecoder(configuration, parentDisposable, context, project)
    }


  }
}

// -- Inner Type ---------------------------------------------------------- //
