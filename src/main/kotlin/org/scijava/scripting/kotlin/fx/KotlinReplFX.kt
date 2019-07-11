package org.scijava.scripting.kotlin.fx

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import org.scijava.Context
import org.scijava.script.ScriptREPL
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory

class KotlinReplFX(context: Context): Closeable {

    companion object {
        private val terminalIds = mutableSetOf<Int>()

        @Synchronized
        private fun getLowestTerminalId(): Int {
            var id = 1
            while (terminalIds.contains(id)) ++id
            terminalIds.add(id)
            return id
        }

        @Synchronized
        private fun freeId(id: Int) = terminalIds.remove(id)
    }

    private val history       = TextArea("")
    private val prompt        = TextArea("")
    private val box           = VBox(history, prompt)
    private val stream        = PrintToStringConsumerStream {history.text = "${history.text}$it"}
    private val repl          = ScriptREPL(context, stream)
    private var count         = 0
    private val terminalId    = getLowestTerminalId()
    private val evalQueue     = Executors.newFixedThreadPool(1) { Thread(it).let { it.name = "Scijava-REPL-FX-$terminalId"; it } }

    init {
        box.maxWidth     = Double.POSITIVE_INFINITY
        history.maxWidth = Double.POSITIVE_INFINITY
        prompt.maxWidth  = Double.POSITIVE_INFINITY

        history.isWrapText = false
        prompt.isWrapText  = false

        prompt.promptText = "In [$count]:"

        history.scrollLeftProperty()

        box.isFillWidth = true

        history.maxHeight = Double.POSITIVE_INFINITY
        if (repl.interpreter === null) {
            repl.lang(repl.interpretedLanguages.first())
        }

        history.isEditable = false
        prompt.isEditable  = true

        history.font = Font.font("Monospace")
        prompt.font  = Font.font("Monospace")

        VBox.setVgrow(history, Priority.ALWAYS)

        repl.initialize(true)

    }

    fun getNode(): Node = box

    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: EventHandler<T>) = prompt.addEventHandler(eventType, eventHandler)


    fun <T: Event> addPromptEventHandler(
            eventType: EventType<T>,
            eventHandler: (T) -> Unit) = addPromptEventHandler(eventType, EventHandler { eventHandler(it) })

    @Synchronized
    fun evalCurrentPrompt(): Future<*> {
        val promptText = prompt.text
        prompt.text = ""
        prompt.promptText = "In [${count+1}]:"
        prompt.isEditable = false
        history.text = "${history.text}\nIn  [$count]: ${promptText}\nOut [$count]: "
        // TODO use queue or block this thread here? could potentially block main application thread? bad?
        return evalQueue.submit { try {
            repl.evaluate(promptText)
        } finally {
            ++count
            prompt.isEditable = true
            history.positionCaret(history.text.length)
        } }
    }

    fun increaseFontSize(factor: Double = 1.1) {
        require(factor > 0, { "Factor > 0 required but received $factor <= 0" })
        val size     = history.font.size
        val font     = Font.font("Monospace", size * factor)
        history.font = font
        prompt.font  = font
    }

    fun decreaseFontSize(factor: Double = 1.1) {
        require(factor > 0, { "Factor > 0 required but received $factor <= 0" })
        val size     = history.font.size
        val font     = Font.font("Monospace", size / factor)
        history.font = font
        prompt.font  = font
    }

    fun shutdown() {
        evalQueue.shutdown()
        freeId(terminalId)
    }

    override fun close() {
        shutdown()
    }



}

fun main() {

    val context     = Context()
    PlatformImpl.startup {}

    val keyTracker = KeyTracker()
    val repl = KotlinReplFX(context)

    repl.getNode().addEventHandler(KeyEvent.KEY_PRESSED) {
        if (
                keyTracker.areOnlyTheseKeysDown(KeyCode.CONTROL, KeyCode.EQUALS) ||
                keyTracker.areOnlyTheseKeysDown(KeyCode.CONTROL, KeyCode.PLUS)) {
            it.consume()
            repl.increaseFontSize()
        } else if (keyTracker.areOnlyTheseKeysDown(KeyCode.CONTROL, KeyCode.MINUS)) {
            it.consume()
            repl.decreaseFontSize()
        }
    }

    repl.addPromptEventHandler(KeyEvent.KEY_PRESSED) {
        if (keyTracker.areOnlyTheseKeysDown(KeyCode.CONTROL, KeyCode.ENTER)) {
            it.consume()
            repl.evalCurrentPrompt()
        }
    }

    val root = StackPane(repl.getNode())
    Platform.runLater {
        val scene = Scene(root, 800.0, 600.0)
        val stage = Stage()
        stage.scene = scene
        keyTracker.installInto(scene)
        stage.show()
    }


}