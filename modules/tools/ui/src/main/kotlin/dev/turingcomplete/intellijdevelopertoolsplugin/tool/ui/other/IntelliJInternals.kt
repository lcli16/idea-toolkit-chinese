package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.lang.UrlClassLoader
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.CopyValuesAction
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginCommonDataKeys
import dev.turingcomplete.intellijdevelopertoolsplugin.common.extension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.simpleColumnInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.setContextMenu
import java.awt.Dimension
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.ListSelectionModel
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.streams.asSequence

class IntelliJInternals(parentDisposable: Disposable, private val project: Project?) :
  DeveloperUiTool(parentDisposable = parentDisposable), DataProvider {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var pluginOverviewTable: JBTable

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    group("Plugins") {
      row {
          val pluginOverviewTableModel =
            ListTableModel<IdeaPluginDescriptor>(*pluginOverviewTableColumns).apply {
              isSortable = true
            }
          pluginOverviewTable =
            JBTable(pluginOverviewTableModel).apply {
              setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
              rowSelectionAllowed = true
              columnSelectionAllowed = false
              setContextMenu(
                this::class.java.name,
                DefaultActionGroup(
                  CopyValuesAction(
                    valueToString = {
                      val ideaPluginDescriptor = it as IdeaPluginDescriptor
                      "${ideaPluginDescriptor.name} (${ideaPluginDescriptor.pluginId.idString})"
                    }
                  ),
                  OpenPluginDirectory(),
                  OpenPluginDescriptor(),
                ),
              )
              setEmptyState("No plugins")
              TableSpeedSearch.installOn(this)
            }
          cell(ScrollPaneFactory.createScrollPane(pluginOverviewTable, false))
            .applyToComponent { preferredSize = Dimension(preferredSize.width, 350) }
            .resizableColumn()
            .align(Align.FILL)
        }
        .bottomGap(BottomGap.NONE)
      row { button("Refresh") { populatePluginOverviewTableModel() } }.topGap(TopGap.NONE)

      group("Find Plugin by Class Name") {
        row {
          val classNameTextField =
            textField().label("Class name:").resizableColumn().align(Align.FILL).component
          button("Find") { findPluginByClassName(classNameTextField.text.trim()) }
        }
      }
    }

    group("Plugin Class Loaders") {
      row {
          cell(
              ScrollPaneFactory.createScrollPane(
                Tree(traverseClassLoader(Thread.currentThread().contextClassLoader)),
                false,
              )
            )
            .align(Align.FILL)
            .resizableColumn()
        }
        .resizableRow()

      group("Find Class File Path by Class Name") {
        row {
          val classNameTextField =
            textField().label("Class name:").resizableColumn().align(Align.FILL).component
          button("Find") { findClassPathByClassName(classNameTextField.text.trim()) }
        }
      }
    }
  }

  override fun afterBuildUi() {
    populatePluginOverviewTableModel()
  }

  override fun getData(dataId: String): Any? =
    when {
      PluginCommonDataKeys.SELECTED_VALUES.`is`(dataId) -> {
        val tableModel =
          pluginOverviewTable.model.uncheckedCastTo<ListTableModel<IdeaPluginDescriptor>>()
        val rowSorter = pluginOverviewTable.rowSorter
        pluginOverviewTable.selectedRows
          .map { tableModel.getRowValue(rowSorter.convertRowIndexToModel(it)) }
          .toList()
      }

      else -> null
    }

  // -- Private Methods ----------------------------------------------------- //

  private fun populatePluginOverviewTableModel() {
    pluginOverviewTable.model.uncheckedCastTo<ListTableModel<IdeaPluginDescriptor>>().items =
      PluginManager.getPlugins().sortedBy { it.name }
  }

  private fun findPluginByClassName(className: String) {
    val messageDialogTitle = "Find Plugin by Class Name"
    try {
      val plugin = PluginManager.getPluginByClass(Class.forName(className))
      if (plugin != null) {
        Messages.showInfoMessage(
          project,
          "Class belongs to plugin: ${plugin.name} (ID: ${plugin.pluginId.idString}).",
          messageDialogTitle,
        )
      } else {
        Messages.showErrorDialog(
          project,
          "No plugin found for the given class name.",
          messageDialogTitle,
        )
      }
    } catch (e: Exception) {
      log.warn("Failed to find plugin", e)
      Messages.showErrorDialog(
        project,
        "${e.message}: ${e::class.qualifiedName}",
        messageDialogTitle,
      )
    }
  }

  private fun findClassPathByClassName(className: String) {
    val messageDialogTitle = "Find Class File Path by Class Name"
    try {
      val aClass = IntelliJInternals::class.java.classLoader.loadClass(className)
      val classFilePath =
        aClass
          .getResource('/' + aClass.getName().replace('.', '/') + ".class")
          ?.toURI()
          ?.path
          ?.toString()
      if (classFilePath != null) {
        Messages.showInfoMessage(project, "Class file path: ${classFilePath}.", messageDialogTitle)
      } else {
        Messages.showErrorDialog(
          project,
          "Unable to resolve the path of the class file for the given class name.",
          messageDialogTitle,
        )
      }
    } catch (e: Exception) {
      log.warn("Failed to find class file", e)
      Messages.showErrorDialog(
        project,
        "${e.message}: ${e::class.qualifiedName}",
        messageDialogTitle,
      )
    }
  }

  private fun traverseClassLoader(classLoader: ClassLoader): DefaultMutableTreeNode {
    val classLoaderNode =
      DefaultMutableTreeNode("Class loader: ${classLoader::class.qualifiedName}")

    if (classLoader is URLClassLoader) {
      classLoader.urLs.forEach { url ->
        classLoaderNode.add(DefaultMutableTreeNode("File: ${Paths.get(url.toURI().path).fileName}"))
      }
    } else if (classLoader is UrlClassLoader) {
      classLoader.files.forEach { file ->
        classLoaderNode.add(DefaultMutableTreeNode("File: ${file.fileName}"))
      }
    }

    val parentClassLoader = classLoader.parent
    if (parentClassLoader != null) {
      val parentClassLoaderNode = traverseClassLoader(parentClassLoader)
      classLoaderNode.add(parentClassLoaderNode)
    }

    return classLoaderNode
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class OpenPluginDirectory : DumbAwareAction("Open Plugin Directory") {

    override fun actionPerformed(e: AnActionEvent) {
      val values: List<Any> =
        PluginCommonDataKeys.SELECTED_VALUES.getData(e.dataContext)
          ?: throw IllegalStateException("snh: Data missing")
      if (values.isEmpty()) {
        return
      }

      values.map { it as PluginDescriptor }.forEach { BrowserUtil.browse(it.pluginPath) }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class OpenPluginDescriptor : DumbAwareAction("Open Plugin Descriptor") {

    override fun actionPerformed(e: AnActionEvent) {
      val project =
        e.dataContext.getData(CommonDataKeys.PROJECT)
          ?: throw IllegalStateException("snh: Data missing")
      val values: List<Any> =
        PluginCommonDataKeys.SELECTED_VALUES.getData(e.dataContext)
          ?: throw IllegalStateException("snh: Data missing")
      if (values.isEmpty()) {
        return
      }

      findPluginDescriptorFiles(values.map { it as IdeaPluginDescriptor }) { pluginDescriptorFiles
        ->
        openProgramDescriptorFile(pluginDescriptorFiles, project)
      }
    }

    private fun findPluginDescriptorFiles(
      pluginDescriptors: List<IdeaPluginDescriptor>,
      callback: (List<VirtualFile>) -> Unit,
    ) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val virtualFileManager = VirtualFileManager.getInstance()
        val pluginDescriptorFiles =
          pluginDescriptors.flatMap { pluginDescriptor ->
            Files.list(pluginDescriptor.pluginPath.resolve("lib")).use { files ->
              files
                .asSequence()
                .filter { Files.isRegularFile(it) && it.extension() == "jar" }
                .mapNotNull {
                  virtualFileManager.findFileByUrl(
                    "jar://${it.toAbsolutePath()}!/${PluginManagerCore.PLUGIN_XML_PATH}"
                  )
                }
                .toList()
            }
          }
        callback(pluginDescriptorFiles)
      }
    }

    private fun openProgramDescriptorFile(
      pluginDescriptorFiles: List<VirtualFile>,
      project: Project,
    ) {
      invokeLater {
        if (pluginDescriptorFiles.isEmpty()) {
          Messages.showErrorDialog(
            project,
            "Unable to find plugin descriptor.",
            "Open Plugin Descriptor",
          )
        }

        val fileEditorManager = FileEditorManager.getInstance(project)
        pluginDescriptorFiles.forEach {
          val openEditor = fileEditorManager.openEditor(OpenFileDescriptor(project, it), true)
          if (openEditor.isEmpty()) {
            Messages.showErrorDialog(
              project,
              "Unable to open file '${it.name}' in editor.",
              "Open Plugin Descriptor",
            )
          }
        }
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<IntelliJInternals> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = "IntelliJ Internals",
        contentTitle = "IntelliJ Internals",
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> IntelliJInternals) = { _ ->
      IntelliJInternals(parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val log = logger<IntelliJInternals>()

    private val pluginOverviewTableColumns: Array<ColumnInfo<IdeaPluginDescriptor, String>> =
      arrayOf(
        simpleColumnInfo("Name", { it.name }, { it.name }),
        simpleColumnInfo("ID", { it.pluginId.idString }, { it.pluginId.idString }),
      )
  }
}
