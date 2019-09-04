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

    private val _history      = TextArea("")
    private val _prompt       = TextArea("")
    private val progress      = ProgressIndicator(1.0)
    private val progressGroup = Group(progress)
    private val historyStack  = StackPane(_history, progressGroup)
    private val box           = VBox(historyStack, _prompt)
    private val stream        = PrintToStringConsumerStream { invokeOnFXApplicationThread { _history.text = "${_history.text}$it" } }
    private val repl          = ScriptREPL(context, stream)
    private var count         = 0

    init {


        box.maxWidth          = Double.POSITIVE_INFINITY
        _history.maxWidth     = Double.POSITIVE_INFINITY
        _prompt.maxWidth      = Double.POSITIVE_INFINITY
        historyStack.maxWidth = Double.POSITIVE_INFINITY

        progress.prefWidth = 50.0

        _history.isWrapText = false
        _prompt.isWrapText  = false

        _prompt.promptText = "In [$count]:"

        box.isFillWidth = true

        _history.maxHeight = Double.POSITIVE_INFINITY
        if (repl.interpreter === null)
            repl.lang(repl.interpretedLanguages.first())

        _history.isEditable = false
        _prompt.isEditable  = true

        _history.font = Font.font("Monospace")
        _prompt.font  = Font.font("Monospace")

        VBox.setVgrow(historyStack, Priority.ALWAYS)
        StackPane.setAlignment(progressGroup, Pos.BOTTOM_RIGHT)
        StackPane.setMargin(progressGroup, Insets(10.0))

        repl.initialize(true)

        _prompt.prefWidthProperty().addListener( InvalidationListener { _prompt.maxHeight = _prompt.prefHeight } )
        _prompt.editableProperty().addListener { _, _, new -> progress.progress = if (new) 1.0 else -1.0 }
        progress.visibleProperty().bind(_prompt.editableProperty().not())

    }

    val node: Node
        get() = box

    val prompt: Node
        get() = _prompt

    val history: Node
        get() = _history

    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: EventHandler<T>) = _prompt.addEventHandler(eventType, eventHandler)


    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: (T) -> Unit) = addPromptEventHandler(eventType, EventHandler { eventHandler(it) })

    @Synchronized
    fun evalCurrentPrompt() {
        val promptText = _prompt.text
        invokeOnFXApplicationThread {
            _prompt.text = ""
            _prompt.positionCaret(0)
            _prompt.promptText = "In [${count + 1}]:"
            _prompt.isEditable = false
            _history.text = "${_history.text}\nIn  [$count]: ${promptText}\nOut [$count]: "
            _history.positionCaret(_history.text.length)
        }
        // TODO use queue or block this thread here? could potentially block main application thread? bad?
        try {
        repl.evaluate(promptText)
        } finally {
            ++count
            invokeOnFXApplicationThread {
                _prompt.isEditable = true
                _history.positionCaret(_history.text.length)
            }
        }
    }

    private fun scaleFontSisze(factor: Double) {
        require(factor > 0, { "Factor > 0 required but received $factor <= 0" })
        val size     = _history.font.size
        val font     = Font.font("Monospace", size * factor)
        _history.font = font
        _prompt.font  = font
    }

    fun increaseFontSize(factor: Double = 1.1) = scaleFontSisze(factor)

    fun decreaseFontSize(factor: Double = 1.1) = scaleFontSisze(1.0 / factor)

    fun setPromptPrefHeight(height: Double) {
        _prompt.prefHeight = height
    }



}