package com.assistant.infera


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

class InferaToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val panel = JPanel()
        val inputArea = JTextArea(5, 40)
        val outputArea = JTextArea(10, 40)
        val button = JButton("Ask AI")

        button.addActionListener {
            val prompt = inputArea.text
            ApplicationManager.getApplication().invokeLater {
                outputArea.text = "[Simulated AI response to]:\n$prompt"
            }
        }

        panel.add(inputArea)
        panel.add(button)
        panel.add(outputArea)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "Infera Code Assistant", false)
        toolWindow.title = "Infera Code Assistant"
        toolWindow.contentManager.addContent(content)
    }
}