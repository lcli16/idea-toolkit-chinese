package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.GroupNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.ToolsMenuTree
import javax.swing.JPanel

open class ContentPanelHandler(
  protected val project: Project?,
  protected val parentDisposable: Disposable,
  settings: DeveloperToolsInstanceSettings,
  groupNodeSelectionEnabled: Boolean = true,
  prioritizeVerticalLayout: Boolean = false,
) {
  // -- Properties ---------------------------------------------------------- //

  private var selectedContentNode = ValueProperty<ContentNode?>(null)

  val contentPanel = BorderLayoutPanel()
  val toolsMenuTree: ToolsMenuTree

  private val cachedGroupsPanels = mutableMapOf<String, GroupContentPanel>()
  private val cachedDeveloperToolsPanels =
    mutableMapOf<DeveloperToolNode, DeveloperToolContentPanel>()

  // -- Initialization ------------------------------------------------------ //

  init {
    // The creation of `ToolsMenuTree` will trigger the initial node selection
    toolsMenuTree =
      ToolsMenuTree(
        project,
        parentDisposable,
        settings,
        groupNodeSelectionEnabled,
        prioritizeVerticalLayout,
      ) { node, selectionTriggeredBySearch ->
        handleContentNodeSelection(node, selectionTriggeredBySearch)
      }
  }

  // -- Exported Methods ---------------------------------------------------- //

  fun <T : OpenDeveloperToolContext> openTool(
    context: T,
    reference: OpenDeveloperToolReference<out T>,
  ) {
    toolsMenuTree.selectDeveloperTool(reference.id) {
      cachedDeveloperToolsPanels[selectedContentNode.get()]?.openTool(context, reference)
    }
  }

  fun showTool(id: String) {
    toolsMenuTree.selectDeveloperTool(id) {}
  }

  protected open fun createDeveloperToolContentPanel(
    developerToolNode: DeveloperToolNode
  ): DeveloperToolContentPanel = DeveloperToolContentPanel(developerToolNode)

  protected open fun handleContentNodeSelection(
    new: ContentNode?,
    selectionTriggeredBySearch: Boolean,
  ) {
    val old = selectedContentNode.get()
    if (old != new) {
      if (old is DeveloperToolNode) {
        cachedDeveloperToolsPanels[old]?.deselected()
        if (!DeveloperToolsApplicationSettings.generalSettings.toolWindowUiCacheUi.get()) {
          cachedDeveloperToolsPanels.remove(old)
        }
      }

      if (new == null) {
        return
      }

      when (new) {
        is GroupNode -> {
          setContentPanel(
            cachedGroupsPanels
              .getOrPut(new.developerUiToolGroup.id) {
                GroupContentPanel(new) {
                  selectedContentNode.set(it)
                  TreeUtil.selectNode(toolsMenuTree, it)
                }
              }
              .panel
          )
          selectedContentNode.set(new)
        }

        is DeveloperToolNode -> {
          setContentPanel(
            cachedDeveloperToolsPanels
              .getOrPut(new) { createDeveloperToolContentPanel(new) }
              .also { it.selected() }
          )
          selectedContentNode.set(new)
        }

        else -> error("Unexpected menu node: ${new::class}")
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun setContentPanel(nodePanel: JPanel) {
    contentPanel.apply {
      removeAll()
      addToCenter(nodePanel)
      revalidate()
      repaint()
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
