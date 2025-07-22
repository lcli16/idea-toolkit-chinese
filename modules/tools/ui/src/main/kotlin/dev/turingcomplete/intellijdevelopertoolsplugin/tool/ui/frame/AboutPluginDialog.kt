package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.IconManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.toolwindow.MainToolWindowFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.RubberDuck
import java.awt.Dimension
import java.awt.Image.SCALE_SMOOTH
import javax.imageio.ImageIO
import javax.swing.Action
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel

class AboutPluginDialog(project: Project?, parentComponent: JComponent) :
  DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    title = UiToolsBundle.message("tools.menu.about-plugin-title", PluginInfo.pluginName)
    isModal = true
    init()
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun createCenterPanel(): JComponent {
    val panel = panel {
      row {
        val tabs =
          mapOf(
            UiToolsBundle.message("tools.menu.about") to createAboutPluginComponent(),
            UiToolsBundle.message("tools.menu.changelog") to createChangelogComponent(),
          )

        cell(
          JBTabbedPane().apply {
            tabs.forEach { (title, component) ->
              // Create scroll panes with specific preferred size
              val scrollPane = ScrollPaneFactory.createScrollPane(component, true)
              scrollPane.preferredSize = Dimension(650, 500)
              addTab(title, scrollPane)
            }
          },
        )
          .align(Align.FILL)
      }
    }

    // Set preferred size on the entire panel
    panel.preferredSize = Dimension(650, 500)
    return panel
  }

  override fun createActions(): Array<Action> = arrayOf(myOKAction)

  // -- Private Methods ----------------------------------------------------- //

  private fun createAboutPluginComponent(): JComponent =
    panel {
      row { text("<b>感谢您使用 ${PluginInfo.pluginName} 插件! ❤\uFE0F </b>") }
        .bottomGap(BottomGap.NONE)
      row { text("版本: ${PluginInfo.pluginVersion}") }.bottomGap(BottomGap.MEDIUM)


      row {
        text(
          "❤\uFE0F 赞助 IntelliJ Toolkit Plugin</h3>",
        ) .gap(RightGap.SMALL)
      } .bottomGap(BottomGap.NONE)

      row {
        text(
          "<p>如果我的项目对你有帮助，或者你想支持我的持续开发，欢迎赞助一杯咖啡或一杯奶茶！你的支持是我前进的最大动力～</p>",
        ) .gap(RightGap.SMALL)
      } .bottomGap(BottomGap.NONE)

      row {
        cell(
          BorderLayoutPanel().apply {
            RubberDuck::class
              .java
              .getResourceAsStream(
                "/dev/turingcomplete/intellijdevelopertoolsplugin/wechat-pay.png"
              )
              ?.use {
                val read = ImageIO.read(it)
                val scaledInstance =
                  read.getScaledInstance(read.width.div(2), read.height.div(2), SCALE_SMOOTH)
                addToCenter(JLabel(ImageIcon(scaledInstance)))
              }
          }
        )
          .align(Align.CENTER)

        cell(
          BorderLayoutPanel().apply {
            RubberDuck::class
              .java
              .getResourceAsStream(
                "/dev/turingcomplete/intellijdevelopertoolsplugin/ali-pay.png"
              )
              ?.use {
                val read = ImageIO.read(it)
                val scaledInstance =
                  read.getScaledInstance(read.width.div(2), read.height.div(2), SCALE_SMOOTH)
                addToCenter(JLabel(ImageIcon(scaledInstance)))
              }
          }
        )
          .align(Align.CENTER)
      }
        .resizableRow()
      row {
        text(
          "发现错误或想到新功能？<a href='https://idea-toolkit-plugin.netlify.app'>请在 GitHub 上创建一个问题。</a>",
        )
          .gap(RightGap.SMALL)
      }
        .bottomGap(BottomGap.NONE)
      row {
        comment(
          "（该插件适用于所有 JetBrains IDE，因此不会扩展针对特定编程语言或框架的功能。插件基于 develop tools 7.1.0 版本开发）",
        )
      }
        .topGap(TopGap.NONE)
    }
      .apply { this.border = JBEmptyBorder(12, 0, 0, 0) }

  private fun createChangelogComponent(): JComponent = panel {
    row {
      text(
        AboutPluginDialog::class.java.getResource(CHANGELOG_HTML_FILE)?.readText()
          ?: "找不到 “最新消息” 文本",
      )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CHANGELOG_HTML_FILE =
      "/dev/turingcomplete/intellijdevelopertoolsplugin/changelog.html"
  }
}
