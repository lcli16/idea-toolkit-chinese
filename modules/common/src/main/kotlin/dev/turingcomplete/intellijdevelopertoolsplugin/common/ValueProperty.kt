package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.dispatcher.SingleEventDispatcher
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import java.util.concurrent.atomic.AtomicReference

open class ValueProperty<T>(initialValue: T) : ObservableMutableProperty<T> {
  // -- Properties ---------------------------------------------------------- //

  private val value = AtomicReference(initialValue)
  private val changeDispatcher = SingleEventDispatcher.Companion.create<ChangeEvent<T>>()
  var modificationsCounter: Int = 0
    private set

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun afterChange(parentDisposable: Disposable?, listener: (T) -> Unit) =
    changeDispatcher.whenEventHappened(parentDisposable) { listener(it.newValue) }

  fun afterChangeConsumeEvent(parentDisposable: Disposable?, listener: (ChangeEvent<T>) -> Unit) =
    changeDispatcher.whenEventHappened(parentDisposable, listener)

  override fun set(value: T) {
    set(value, null)
  }

  fun setWithUncheckedCast(value: Any, changeId: String?) {
    @Suppress("UNCHECKED_CAST") set(value as T, changeId)
  }

  fun set(newValue: T, changeId: String?, fireEvent: Boolean = true) {
    val oldValue = this.value.getAndSet(newValue)

    if (oldValue != newValue) {
      modificationsCounter++
    }

    if (fireEvent) {
      changeDispatcher.fireEvent(ChangeEvent(changeId, oldValue, newValue))
    }
  }

  override fun get(): T = this.value.get()

  override fun toString(): String = get().toString()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class ChangeEvent<T>(val id: String?, val oldValue: T, val newValue: T) {

    fun valueChanged() = oldValue != newValue
  }

  // -- Companion Object ---------------------------------------------------- //
}
