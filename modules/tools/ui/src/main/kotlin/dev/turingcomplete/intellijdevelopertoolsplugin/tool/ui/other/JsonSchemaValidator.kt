package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.SpecVersionDetector
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate

class JsonSchemaValidator(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private var liveValidation = configuration.register("liveValidation", true)
  private val schemaText =
    configuration.register("schemaText", "", PropertyType.INPUT, EXAMPLE_SCHEMA)
  private val dataText = configuration.register("dataText", "", PropertyType.INPUT, EXAMPLE_DATA)

  private val schemaEditor by lazy { this.createSchemaEditor() }
  private val schemaErrorHolder = ErrorHolder()
  private val dataEditor by lazy { this.createDataEditor() }
  private val dataErrorHolder = ErrorHolder()

  private val validationState: ObservableMutableProperty<ValidationState> =
    AtomicProperty(ValidationState.VALIDATED)
  private val validationError: ObservableMutableProperty<String> = AtomicProperty("")

  // -- Initialization ------------------------------------------------------ //

  init {
    liveValidation.afterChange {
      if (it) {
        validateSchema()
      }
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        cell(schemaEditor.component)
          .align(Align.FILL)
          .validationOnApply(schemaEditor.bindValidator(schemaErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
      }
      .resizableRow()

    row {
      val liveValidationCheckBox =
        checkBox("实时验证").bindSelected(liveValidation).gap(RightGap.SMALL)

      button("验证") { validateSchema() }
        .enabledIf(liveValidationCheckBox.selected.not())
        .gap(RightGap.SMALL)
    }

    row {
        cell(dataEditor.component)
          .align(Align.FILL)
          .validationOnApply(dataEditor.bindValidator(dataErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
      }
      .resizableRow()

    row {
        icon(AllIcons.General.InspectionsOK).gap(RightGap.SMALL)
        label("数据匹配架构")
      }
      .visibleIf(PropertyComponentPredicate(validationState, ValidationState.VALIDATED))

    row {
        icon(AllIcons.General.BalloonError).gap(RightGap.SMALL)
        label("").bindText(validationError)
      }
      .visibleIf(PropertyComponentPredicate(validationState, ValidationState.ERROR))

    row {
        icon(AllIcons.General.Warning).gap(RightGap.SMALL)
        label("输入无效")
      }
      .visibleIf(PropertyComponentPredicate(validationState, ValidationState.INVALID_INPUT))
  }

  override fun afterBuildUi() {
    if (liveValidation.get()) {
      validateSchema()
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun validateSchema() {
    schemaErrorHolder.clear()
    dataErrorHolder.clear()

    val jsonMapper = ObjectMapperService.instance.jsonMapper()

    val schema: JsonSchema? =
      try {
        val schemaNode = jsonMapper.readTree(schemaEditor.text)
        val versionFlag = SpecVersionDetector.detect(schemaNode)
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaEditor.text)
      } catch (e: Exception) {
        schemaErrorHolder.add(e)
        validationState.set(ValidationState.INVALID_INPUT)
        null
      }

    val dataNode: JsonNode? =
      try {
        jsonMapper.readTree(dataEditor.text)
      } catch (e: Exception) {
        dataErrorHolder.add(e)
        validationState.set(ValidationState.INVALID_INPUT)
        null
      }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()

    if (schema != null && dataNode != null) {
      val errors = schema.validate(dataNode)
      if (errors.isEmpty()) {
        validationState.set(ValidationState.VALIDATED)
      } else {
        validationState.set(ValidationState.ERROR)
        validationError.set(
          """
            <html>
            数据与架构不匹配:<br />
              ${errors.joinToString(separator = "<br />") { "- $it" }}
            </html>
          """
            .trimIndent()
        )
      }
    }
  }

  private fun createSchemaEditor() =
    AdvancedEditor(
        id = "schema",
        context = context,
        configuration = configuration,
        project = project,
        title = "JSON 格式",
        editorMode = INPUT,
        parentDisposable = parentDisposable,
        initialLanguage = JsonLanguage.INSTANCE,
        textProperty = schemaText,
      )
      .apply {
        onTextChangeFromUi {
          if (liveValidation.get()) {
            validateSchema()
          }
        }
      }

  private fun createDataEditor() =
    AdvancedEditor(
        id = "data",
        context = context,
        configuration = configuration,
        project = project,
        title = "JSON 数据",
        editorMode = INPUT,
        parentDisposable = parentDisposable,
        initialLanguage = JsonLanguage.INSTANCE,
        textProperty = dataText,
      )
      .apply {
        onTextChangeFromUi {
          if (liveValidation.get()) {
            validateSchema()
          }
        }
      }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class ValidationState {

    VALIDATED,
    ERROR,
    INVALID_INPUT,
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<JsonSchemaValidator> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "JSON 格式", contentTitle = "JSON格式验证器")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> JsonSchemaValidator) = { configuration ->
      JsonSchemaValidator(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val EXAMPLE_SCHEMA =
      """
{
  "${'$'}id": "https://example.com/person.schema.json",
  "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Person",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string",
      "description": "The person's first name."
    },
    "lastName": {
      "type": "string",
      "description": "The person's last name."
    },
    "age": {
      "description": "Age in years which must be equal to or greater than zero.",
      "type": "integer",
      "minimum": 0
    }
  }
}
    """
        .trimIndent()
    private val EXAMPLE_DATA =
      """
{
  "firstName": "John",
  "lastName": "Doe",
  "age": 21
}
    """
        .trimIndent()
  }
}
