package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.transform
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Base64
import java.util.HexFormat
import java.util.Locale.getDefault
import kotlin.reflect.KClass
import kotlin.reflect.cast

// -- Properties ---------------------------------------------------------- //

val longMaxValue = BigDecimal(Long.MAX_VALUE)
val longMinValue = BigDecimal(Long.MIN_VALUE)

val emptyByteArray = ByteArray(0)

private val asciiEncodedRegex = "\\\\u([0-9a-fA-F]{4})".toRegex()

// -- Initialization ------------------------------------------------------ //

// -- Exported Methods ---------------------------------------------------- //

inline fun <reified T> Any.safeCastTo(): T? = this as? T

fun <T : Any> Any.uncheckedCastTo(type: KClass<T>): T = type.cast(this)

inline fun <reified T> Any.uncheckedCastTo(): T = this as T

fun ByteArray.toHexMacAddress() =
  StringBuilder(18)
    .also {
      for (byte in this) {
        if (isNotEmpty()) {
          it.append(':')
        }
        it.append(String.format("%02x", byte))
      }
    }
    .toString()

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)

fun String.toMessageDigest(): MessageDigest = MessageDigest.getInstance(this)

fun String.toLowerCasePreservingASCIIRules(): String =
  this.map {
      when (it) {
        in 'A'..'Z' -> it + 32
        else -> it
      }
    }
    .joinToString("")

fun Comparator<String>.makeCaseInsensitive(): Comparator<String> {
  return Comparator { a, b ->
    this.compare(a.toLowerCasePreservingASCIIRules(), b.toLowerCasePreservingASCIIRules())
  }
}

fun Long?.compareTo(other: Long?): Int {
  return if (this == null && other == null) {
    0
  } else if (this == null) {
    1
  } else if (other == null) {
    -1
  } else {
    this.compareTo(other)
  }
}

fun String.encodeToAscii() =
  this.map { if (it.code > 127) "\\u%04x".format(it.code) else it.toString() }.joinToString("")

fun ByteArray.encodeToAscii(): ByteArray =
  this.joinToString("") {
      if (it.toInt() and 0xFF > 127) "\\u%04x".format(it.toInt() and 0xFF)
      else it.toInt().toChar().toString()
    }
    .toByteArray()

fun String.decodeFromAscii() =
  asciiEncodedRegex.replace(this) { matchResult ->
    val charCode = matchResult.groupValues[1].toInt(16)
    charCode.toChar().toString()
  }

fun ByteArray.decodeFromAscii(): ByteArray =
  asciiEncodedRegex
    .replace(this.toString(Charsets.UTF_8)) { matchResult ->
      val charCode = matchResult.groupValues[1].toInt(16)
      charCode.toChar().toString()
    }
    .toByteArray()

fun String.decodeBase64String(): String =
  if (this.isNotEmpty()) String(Base64.getDecoder().decode(this)) else ""

fun MatchGroupCollection.getOrNull(index: Int): MatchGroup? =
  if (index in 0 until size) this[index] else null

fun <T : Enum<T>> KClass<T>.findEnumValueByName(name: String): T? =
  this.java.enumConstants?.find { it.name == name }

fun <T : Enum<T>> KClass<T>.getEnumValueByNameOrThrow(name: String): T =
  this.java.enumConstants?.find { it.name == name } ?: error("Enum value $name not found in $this")

operator fun ObservableMutableProperty<Boolean>.not(): ObservableMutableProperty<Boolean> =
  transform({ !it }) { !it }

fun BigDecimal.isWithinLongRange(): Boolean = (this <= longMaxValue) && (this >= longMinValue)

fun <T : Enum<T>> KClass<T>.valueOf(name: String): T {
  return this.java.enumConstants?.firstOrNull { it.name == name }
    ?: error("Enum $this does not have a constant with name: $name")
}

inline fun <T, U> Iterable<T>.ifNotEmpty(action: Iterable<T>.() -> U): U? =
  if (this.any()) action(this) else null

inline fun <T> Collection<T>.random(except: (T) -> Boolean): T =
  this.filter { !except(it) }.random()

inline fun <T> Array<T>.random(except: (T) -> Boolean): T = this.filter { !except(it) }.random()

fun String.capitalize() = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
}

// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //
