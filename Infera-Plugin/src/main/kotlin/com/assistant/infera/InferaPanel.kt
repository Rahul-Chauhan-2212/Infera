package com.assistant.infera

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class InferaPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val editor: Editor

    private val suggestButton = JButton("Suggest").apply {
        addActionListener { handleSuggestion() }
    }

    init {
        // Top panel with label and button
        val inputLabel = JLabel("Click to get suggestion for current editor code:")
        val topPanel = JPanel(BorderLayout()).apply {
            add(inputLabel, BorderLayout.WEST)
            add(suggestButton, BorderLayout.EAST)
        }
        add(topPanel, BorderLayout.NORTH)

        // Code editor panel
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("")
        editor = editorFactory.createEditor(document, project)

        (editor as EditorEx).isViewer = true

        // Add syntax highlighting
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("java")
        val colorScheme = EditorColorsManager.getInstance().globalScheme
        val highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(fileType, colorScheme, project)
        (editor as EditorEx).highlighter = highlighter

        // Editor UI settings
        editor.settings.apply {
            isCaretRowShown = true
            isLineNumbersShown = true
            isFoldingOutlineShown = false
            isLineMarkerAreaShown = false
            isUseSoftWraps = true
        }

        val editorPanel = JBPanel<Nothing>().apply {
            layout = BorderLayout()
            add(editor.component, BorderLayout.CENTER)
        }

        add(JBScrollPane(editorPanel), BorderLayout.CENTER)
    }

    private fun handleSuggestion() {
        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
        if (activeEditor == null) {
            setEditorContent("// No active editor found.")
            return
        }

        val code = activeEditor.document.text
        val line = activeEditor.caretModel.logicalPosition.line
        val language = getLanguageOfCurrentFile()

        setEditorContent("") // Clear previous output
        suggestButton.isEnabled = false

        SuggestionService.fetchStreamingSuggestion(
            code = code,
            line = line,
            language = language,
            onToken = { token ->
                ApplicationManager.getApplication().invokeLater {
                    appendToEditor(token)
                }
            },
            onDone = {
                ApplicationManager.getApplication().invokeLater {
                    suggestButton.isEnabled = true
                }
            }
        )
    }

    private fun setEditorContent(content: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(content)
        }
    }

    private fun appendToEditor(token: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.insertString(editor.document.textLength, token)
        }
    }

    private fun getLanguageOfCurrentFile(): String {
        val virtualFile: VirtualFile =
            FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: return "Unknown"
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return "Unknown"
        return psiFile.language.displayName
    }

    fun updateCodeBlock(markdownCode: String) {
        val code = extractCodeFromMarkdown(markdownCode)
        setEditorContent(code)
    }

    private fun extractCodeFromMarkdown(codeBlock: String): String {
        val regex = Regex("```(?:[a-zA-Z]*)\\s*([\\s\\S]*?)```")
        val match = regex.find(codeBlock)
        return match?.groups?.get(1)?.value?.trim() ?: codeBlock.trim()
    }

    fun disposeEditor() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}