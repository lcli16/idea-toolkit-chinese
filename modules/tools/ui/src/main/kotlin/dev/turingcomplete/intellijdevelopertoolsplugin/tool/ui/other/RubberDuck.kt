package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import java.awt.Image.SCALE_SMOOTH
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel

class RubberDuck(parentDisposable: Disposable) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(
        JBLabel(
          """<html>
             橡皮鸭调试是一种解决问题的技术，程序员通过以下方式解释他们的代码行
             line 附加到 Rubber Duck 或任何其他无生命的物体。解释代码的行为会有所帮助
             程序员识别其代码中的错误和逻辑错误。这种技术被广泛使用
             用于软件开发，以提高代码质量和调试效率。
            </html>"""
            .trimMargin()
        )
      )
    }

    row {
        cell(
            BorderLayoutPanel().apply {
              RubberDuck::class
                .java
                .getResourceAsStream(
                  "/dev/turingcomplete/intellijdevelopertoolsplugin/rubber-duck-yellow.png"
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
      comment(
        "图片由 <a href='https://www.pexels.com/photo/yellow-duck-toy-beside-green-duck-toy-132464/'>Anthony</a>"
      ) {
        BrowserUtil.browse(it.url)
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<RubberDuck> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "橡皮鸭", contentTitle = "橡皮鸭调试")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> RubberDuck) = { RubberDuck(parentDisposable) }
  }

  // -- Companion Object ---------------------------------------------------- //
}
