package org.scijava.scripting.fx

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.scijava.Context

private fun smallestId(set: Set<Int>, min: Int = 1): Int {
    var start = min
    while (set.contains(start)) ++start
    return start
}

fun main() {

    val context     = Context()
    PlatformImpl.startup {}

    val increaseFontKeys = setOf(KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY))
    val decreaseFontKeys = setOf(KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY))
    val evalKeys = setOf(KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN))
    val exitKeyCombination = setOf(KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN))


    val tabPane = TabPane()

    val root = StackPane(tabPane)
    Platform.runLater {
        val scene = Scene(root, 800.0, 600.0)
        val stage = Stage()
        stage.scene = scene
        stage.show()
    }

    val replIds = FXCollections.observableHashMap<Int, Pair<SciJavaReplFX, Tab>>()
    replIds.addListener(MapChangeListener<Int, Pair<SciJavaReplFX, Tab>> {
        if (it.wasRemoved())
            tabPane.tabs.remove(it.valueRemoved.second)
        else if (it.wasAdded()) {
            tabPane.tabs.add(it.valueAdded.second)
            tabPane.selectionModel.select(it.valueAdded.second)
        }
    })

    val createAndAddRepl = {
        val repl = SciJavaReplFX(context)

        repl.setPromptPrefHeight(250.0)

        repl.getNode().addEventHandler(KeyEvent.KEY_PRESSED) {
            if (increaseFontKeys.any { c -> c.match(it) }) {
                it.consume()
                repl.increaseFontSize()
            } else if (decreaseFontKeys.any { c -> c.match(it) }) {
                it.consume()
                repl.decreaseFontSize()
            }
        }

        repl.addPromptEventHandler(KeyEvent.KEY_PRESSED) {
            if (evalKeys.any { c -> c.match(it) }) {
                it.consume()
                GlobalScope.launch { repl.evalCurrentPrompt() }
            }
        }
        synchronized(replIds) {
            val replId = smallestId(replIds.keys)
            replIds[replId] = Pair(repl, Tab("REPL $replId", repl.getNode()))

            repl.addPromptEventHandler(KeyEvent.KEY_PRESSED) {
                if (exitKeyCombination.any { c -> c.match(it) }) {
                    it.consume()
                    synchronized(replIds) {
                        replIds.remove(replId)
                    }
                }
            }
        }
    }

    createAndAddRepl()

    tabPane.addEventHandler(KeyEvent.KEY_PRESSED) {
        if (KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(it)) {
            it.consume()
            createAndAddRepl()
        }
    }


}