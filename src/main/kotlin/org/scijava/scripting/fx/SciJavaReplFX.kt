package org.scijava.scripting.fx

import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import org.scijava.Context
import org.scijava.script.ScriptREPL

private fun invokeOnFXApplicationThread(task: () -> Unit) = invokeOnFXApplicationThread(Runnable { task() })

private fun invokeOnFXApplicationThread(task: Runnable) {
    if (Platform.isFxApplicationThread())
        task.run()
    else
        Platform.runLater(task)
}

class SciJavaReplFX(context: Context) {

    private val history       = TextArea("")
    private val prompt        = TextArea("")
    private val progress      = ProgressIndicator(1.0)
    private val progressGroup = Group(progress)
    private val historyStack  = StackPane(history, progressGroup)
    private val box           = VBox(historyStack, prompt)
    private val stream        = PrintToStringConsumerStream { invokeOnFXApplicationThread { history.text = "${history.text}$it" } }
    private val repl          = ScriptREPL(context, stream)
    private var count         = 0

    init {


        box.maxWidth          = Double.POSITIVE_INFINITY
        history.maxWidth      = Double.POSITIVE_INFINITY
        prompt.maxWidth       = Double.POSITIVE_INFINITY
        historyStack.maxWidth = Double.POSITIVE_INFINITY

        progress.prefWidth = 50.0

        history.isWrapText = false
        prompt.isWrapText  = false

        prompt.promptText = "In [$count]:"

        box.isFillWidth = true

        history.maxHeight = Double.POSITIVE_INFINITY
        if (repl.interpreter === null) {
            repl.lang(repl.interpretedLanguages.first())
        }

        history.isEditable = false
        prompt.isEditable  = true

        history.font = Font.font("Monospace")
        prompt.font  = Font.font("Monospace")

        VBox.setVgrow(historyStack, Priority.ALWAYS)
        StackPane.setAlignment(progressGroup, Pos.BOTTOM_RIGHT)
        StackPane.setMargin(progressGroup, Insets(10.0))

        repl.initialize(true)

        prompt.prefWidthProperty().addListener( InvalidationListener { prompt.maxHeight = prompt.prefHeight } )
        prompt.editableProperty().addListener { obs, oldv, newv -> progress.progress = if (newv) 1.0 else -1.0 }
        progress.visibleProperty().bind(prompt.editableProperty().not())

    }

    fun getNode(): Node = box

    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: EventHandler<T>) = prompt.addEventHandler(eventType, eventHandler)


    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: (T) -> Unit) = addPromptEventHandler(eventType, EventHandler { eventHandler(it) })

    @Synchronized
    fun evalCurrentPrompt() {
        val promptText = prompt.text
        invokeOnFXApplicationThread {
            prompt.text = ""
            prompt.positionCaret(0)
            prompt.promptText = "In [${count + 1}]:"
            prompt.isEditable = false
            history.text = "${history.text}\nIn  [$count]: ${promptText}\nOut [$count]: "
            history.positionCaret(history.text.length)
        }
        // TODO use queue or block this thread here? could potentially block main application thread? bad?
        try {
        repl.evaluate(promptText)
        } finally {
            ++count
            invokeOnFXApplicationThread {
                prompt.isEditable = true
                history.positionCaret(history.text.length)
            }
        }
    }

    private fun scaleFontSisze(factor: Double) {
        require(factor > 0, { "Factor > 0 required but received $factor <= 0" })
        val size     = history.font.size
        val font     = Font.font("Monospace", size * factor)
        history.font = font
        prompt.font  = font
    }

    fun increaseFontSize(factor: Double = 1.1) = scaleFontSisze(factor)

    fun decreaseFontSize(factor: Double = 1.1) = scaleFontSisze(1.0 / factor)

    fun setPromptPrefHeight(height: Double) {
        prompt.prefHeight = height
    }



}