package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.fasterxml.uuid.Generators
import com.github.f4b6a3.ulid.UlidCreator
import dev.turingcomplete.intellijdevelopertoolsplugin.common.HashingUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.I18nUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.annotations.Nls
import kotlin.collections.sorted

object DataGenerators {
  // -- Variables ----------------------------------------------------------- //

  private val uuidV7Generator = UuidV7Generator()
  val dataGenerators: List<DataGeneratorBase> =
    listOf(
      UuidV4Generator(),
      uuidV7Generator,
      UlidGenerator(),
      NanoIdGenerator(),
      DataGeneratorsGroup(UiToolsBundle.message("data-generator.current-datetime-title") , createCurrentDateAndTimeGenerators()),
      DataGeneratorsGroup(UiToolsBundle.message("data-generator.current-unix-timestamp-title"), createUnixTimestampGenerators()),
      DataGeneratorsGroup(UiToolsBundle.message("data-generator.random-Hash-title"), createRandomHashGenerators()),
    )

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //

  private fun createRandomHashGenerators(): List<DataGenerator> =
    HashingUtils.commonMessageDigests.map { messageDigest ->
      object :
        DataGenerator(messageDigest.algorithm, UiToolsBundle.message(
          "data-generator.generate-random-title",
          "${messageDigest.algorithm}"
        ) ) {

        override fun generate(): String =
          messageDigest.digest(uuidV7Generator.generate().encodeToByteArray()).toHexString()
      }
    }

  private fun createUnixTimestampGenerators(): List<DataGenerator> =
    linkedMapOf(
      UiToolsBundle.message("data-generator.current-unix-seconds-title")  to { System.currentTimeMillis().div(1000).toString() },
      UiToolsBundle.message("data-generator.current-unix-milliseconds-title") to { System.currentTimeMillis().toString() },
      UiToolsBundle.message("data-generator.current-unix-nanoseconds-title")   to { System.nanoTime().toString() },
      )
      .map { (name, generateUnixTimestamp) ->
        object : DataGenerator(name, UiToolsBundle.message(
          "data-generator.insert-current-unix-timestamp-title",
         "(${name.lowercase()})"
        )) {

          override fun generate(): String = generateUnixTimestamp()
        }
      }

  private fun createCurrentDateAndTimeGenerators(): List<DataGenerator> =
    listOf(
        Triple(
          "ISO-8601 date time with time zone",
          "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
          ZoneId.systemDefault(),
        ),
        Triple("ISO-8601 date time at UTC", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", ZoneOffset.UTC),
        Triple("ISO-8601 date", "yyyy-MM-dd", ZoneId.systemDefault()),
        Triple("ISO-8601 time", "HH:mm:ss", ZoneId.systemDefault()),
        Triple("ISO-8601 ordinal date", "yyyy-DDD", ZoneId.systemDefault()),
        Triple("ISO-8601 week date", "YYYY-'W'ww-e", ZoneId.systemDefault()),
        Triple("RFC-1123 date time", "EEE, dd MMM yyyy HH:mm:ss", ZoneOffset.UTC),
      )
      .map { (name, pattern, timeZone) ->
        object : DataGenerator(pattern, name, UiToolsBundle.message(
          "data-generator.insert-current-datetime-title",
          pattern
        )) {

          override fun generate(): String =
            DateTimeFormatter.ofPattern(pattern)
              .withLocale(Locale.getDefault())
              .withZone(timeZone)
              .format(Instant.now())
        }
      }


  // -- Inner Type ---------------------------------------------------------- //

  class UuidV4Generator : DataGenerator("UUIDv4") {

    override fun generate(): String = Generators.randomBasedGenerator().generate().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class UuidV7Generator : DataGenerator("UUIDv7") {

    override fun generate(): String = Generators.timeBasedEpochGenerator().generate().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class UlidGenerator : DataGenerator("ULID") {

    override fun generate(): String = UlidCreator.getUlid().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class NanoIdGenerator : DataGenerator("Nano ID") {

    override fun generate(): String = NanoIdUtils.randomNanoId()
  }

  // -- Inner Type ---------------------------------------------------------- //

  sealed interface DataGeneratorBase {

    val title: String
    val toolText: String?
  }

  // -- Inner Type ---------------------------------------------------------- //

  abstract class DataGenerator(
    @Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val actionName: String = UiToolsBundle.message(
      "data-generator.insert-generated-title",
      title
    ),
    @Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null,
  ) : DataGeneratorBase {

    abstract fun generate(): String
  }

  // -- Inner Type ---------------------------------------------------------- //

  class DataGeneratorsGroup(
    @Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val children: List<DataGeneratorBase>,
    @Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null,
  ) : DataGeneratorBase
}
