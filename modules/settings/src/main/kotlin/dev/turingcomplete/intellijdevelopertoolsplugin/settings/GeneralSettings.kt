package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.CommonBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.ACTION_HANDLING_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.ADVANCED_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.DEFAULT_EDITOR_SETTINGS_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.EnumSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.EnumValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.InternalSetting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Setting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle

@SettingsGroup(
  id = DEFAULT_EDITOR_SETTINGS_GROUP_ID,
  titleBundleKey = "general-settings.default-editor-settings-group.title",
  order = 0,
)
@SettingsGroup(
  id = ACTION_HANDLING_GROUP_ID,
  titleBundleKey = "general-settings.action-handling-group.title",
  descriptionBundleKey = "general-settings.action-handling-group.description",
  order = 1,
)
@SettingsGroup(
  id = ADVANCED_GROUP_ID,
  titleBundleKey = "general-settings.advanced-group.title",
  order = 2,
)
interface GeneralSettings : Settings {
  // -- Properties ---------------------------------------------------------- //

  @Setting(
    titleBundleKey = "general-settings.add-open-main-dialog-action-to-main-toolbar.title",
    descriptionBundleKey =
      "general-settings.add-open-main-dialog-action-to-main-toolbar.description",
    order = 0,
  )
  @BooleanValue(defaultValue = false)
  val addOpenMainDialogActionToMainToolbar: BooleanSettingProperty

  @Setting(titleBundleKey = "general-settings.load-examples.title", order = 1)
  @BooleanValue(defaultValue = true)
  val loadExamples: BooleanSettingProperty

  @Setting(titleBundleKey = "general-settings.save-configurations.title", order = 2)
  @BooleanValue(defaultValue = true)
  val saveConfigurations: BooleanSettingProperty

  @Setting(titleBundleKey = "general-settings.save-inputs.title", order = 3)
  @BooleanValue(defaultValue = true)
  val saveInputs: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.save-sensitive-inputs.title",
    descriptionBundleKey = "general-settings.save-sensitive-inputs.description",
    order = 4,
  )
  @BooleanValue(defaultValue = false)
  val saveSensitiveInputs: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.editor-soft-wraps.title",
    groupId = DEFAULT_EDITOR_SETTINGS_GROUP_ID,
    order = 5,
  )
  @BooleanValue(defaultValue = true)
  val editorSoftWraps: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.editor-show-special-characters.title",
    groupId = DEFAULT_EDITOR_SETTINGS_GROUP_ID,
    order = 6,
  )
  @BooleanValue(defaultValue = false)
  val editorShowSpecialCharacters: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.editor-show-whitespaces.title",
    groupId = DEFAULT_EDITOR_SETTINGS_GROUP_ID,
    order = 7,
  )
  @BooleanValue(defaultValue = false)
  val editorShowWhitespaces: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.tools-menu-tree-show-group-nodes.title",
    groupId = ADVANCED_GROUP_ID,
    order = 8,
  )
  @BooleanValue(defaultValue = false)
  val toolsMenuTreeShowGroupNodes: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.tools-menu-tree-order-alphabetically.title",
    groupId = ADVANCED_GROUP_ID,
    order = 9,
  )
  @BooleanValue(defaultValue = true)
  val toolsMenuTreeOrderAlphabetically: BooleanSettingProperty

  @InternalSetting(groupId = ACTION_HANDLING_GROUP_ID)
  @BooleanValue(defaultValue = true)
  val autoDetectActionHandlingInstance: BooleanSettingProperty

  @InternalSetting(groupId = ACTION_HANDLING_GROUP_ID)
  @EnumValue<ActionHandlingInstance>(
    enumClass = ActionHandlingInstance::class,
    defaultValueName = "TOOL_WINDOW",
    displayTextProvider = ActionHandlingInstanceDisplayTextProvider::class,
  )
  val selectedActionHandlingInstance: EnumSettingProperty<ActionHandlingInstance>

  @Setting(
    titleBundleKey = "general-settings.show-internal-tools.title",
    groupId = ADVANCED_GROUP_ID,
    descriptionBundleKey = "general-settings.show-internal-tools.description",
    order = 10,
  )
  @BooleanValue(defaultValue = false)
  val showInternalTools: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.hide-workbench-tabs-on-single-tab.title",
    groupId = ADVANCED_GROUP_ID,
    order = 11,
  )
  @BooleanValue(defaultValue = true)
  val hideWorkbenchTabsOnSingleTab: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.dialog-is-modal.title",
    groupId = ADVANCED_GROUP_ID,
    order = 12,
  )
  @BooleanValue(defaultValue = false)
  val dialogIsModal: BooleanSettingProperty

  @Setting(
    titleBundleKey = "general-settings.tool-window-cache-ui.title",
    descriptionBundleKey = "general-settings.tool-window-cache-ui.description",
    groupId = ADVANCED_GROUP_ID,
    order = 13,
  )
  @BooleanValue(defaultValue = false)
  val toolWindowUiCacheUi: BooleanSettingProperty

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //

  // -- Inner Type ---------------------------------------------------------- //

  enum class ActionHandlingInstance(val displayText: String) {

    TOOL_WINDOW(SettingsBundle.message("general-settings.action-handling-instance.tool-window")),
    DIALOG(SettingsBundle.message("general-settings.action-handling-instance.dialog"));

    override fun toString(): String = displayText
  }

  // -- Inner Type ---------------------------------------------------------- //

  class ActionHandlingInstanceDisplayTextProvider :
    EnumValue.DisplayTextProvider<ActionHandlingInstance> {

    override fun toDisplayText(value: ActionHandlingInstance): String = value.displayText
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val DEFAULT_EDITOR_SETTINGS_GROUP_ID = "defaultEditorSettings"
    private const val ADVANCED_GROUP_ID = "advanced"
    private const val EXPERIMENTAL_GROUP_ID = "experimental"
    const val ACTION_HANDLING_GROUP_ID = "actionHandling"

    fun GeneralSettings.createSensitiveInputsHandlingToolTipText(): String? =
      if (saveInputs.get() && !saveSensitiveInputs.get()) {
        "<html>This sensitive input field will be cleared after the application is closed.<br />" +
          "You can deactivate this behavior in the ${CommonBundle.settingsTitle().lowercase()}.</html>"
      } else {
        null
      }
  }
}
