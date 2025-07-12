package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.observable.dispatcher.SingleEventDispatcher
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.ui.layout.ComponentPredicate

class PropertyComponentPredicate<T>(
  private val property: ObservableProperty<T>,
  private val expectedValue: T,
) : ComponentPredicate() {
  // -- Properties ---------------------------------------------------------- //

  private val changeDispatcher = SingleEventDispatcher.create<T>()

  // -- Initialization ------------------------------------------------------ //

  init {
    property.afterChange { value -> changeDispatcher.fireEvent(value) }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun addListener(listener: (Boolean) -> Unit) {
    changeDispatcher.whenEventHappened { value -> listener(value?.equals(expectedValue) ?: false) }
  }

  override fun invoke(): Boolean = property.get()?.equals(expectedValue) ?: false

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    fun <T> ObservableProperty<T>.createPredicate(expectedValue: T): PropertyComponentPredicate<T> =
      PropertyComponentPredicate(this, expectedValue)
  }
}
