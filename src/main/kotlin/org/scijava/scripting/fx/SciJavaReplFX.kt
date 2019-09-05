/*-
 * #%L
 * JavaFX frontend for SciJava JSR-223-compliant scripting plugins.
 * %%
 * Copyright (C) 2019 HHMI Janelia Research Campus.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.scripting.fx

import javafx.application.Platform
import javafx.beans.InvalidationListener
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

    @Synchronized
    fun evalCurrentPrompt() {
        val promptText = _prompt.text
        invokeOnFXApplicationThread {
            _prompt.text = ""
            _prompt.positionCaret(0)
            _prompt.promptText = "In [${count + 1}]:"
            _prompt.isEditable = false
            _history.text = "${_history.text}\nIn  [$count]: $promptText\nOut [$count]: "
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
        require(factor > 0) { "Factor > 0 required but received $factor <= 0" }
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

    fun putBindings(bindings: Map<String, *>) = repl.interpreter.bindings.putAll(bindings)

    fun putBindings(vararg bindings: Pair<String, *>) = putBindings(mapOf(*bindings))



}
