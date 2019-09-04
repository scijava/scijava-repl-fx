package org.scijava.scripting.fx

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.scijava.Context

class ScijavaReplFXTabs(
        private val context: Context,
        private val increaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
        private val decreaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
        private val evalKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)),
        private val exitKeyCombination: Collection<KeyCombination> = setOf (KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)),
        private val createNewReplCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
        private val cycleTabsForwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN)),
        private val cycleTabsBackwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
        private val bindings: Map<String, *> = mapOf<String, Any>()
) {

    constructor(
            context: Context,
            increaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
            decreaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
            evalKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)),
            exitKeyCombination: Collection<KeyCombination> = setOf (KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)),
            createNewReplCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
            cycleTabsForwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN)),
            cycleTabsBackwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
            vararg bindings: Pair<String, *>
    ) : this(
            context,
            increaseFontKeys,
            decreaseFontKeys,
            evalKeys,
            exitKeyCombination,
            createNewReplCombination,
            cycleTabsForwardKombination,
            cycleTabsBackwardKombination,
            mapOf(*bindings))


    private val tabPane = TabPane()
            .also { it.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS }

    val node: Node
        get() = tabPane

    private val replIds = HashMap<Int, Pair<SciJavaReplFX, Tab>>()

    init {
        tabPane.addEventHandler(KeyEvent.KEY_PRESSED)
        { ev ->
            if (createNewReplCombination.any { it.match(ev) }) {
                ev.consume()
                createAndAddTab()
            }
        }
    }

    fun createAndAddTab()  {
        val repl = SciJavaReplFX(context)
                .also { it.setPromptPrefHeight(250.0) }
                .also { it.putBindings(bindings) }

        repl.node.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (increaseFontKeys.any { c -> c.match(it) }) {
                it.consume()
                repl.increaseFontSize()
            } else if (decreaseFontKeys.any { c -> c.match(it) }) {
                it.consume()
                repl.decreaseFontSize()
            }
        }

        repl.prompt.addEventHandler(KeyEvent.KEY_PRESSED) {
            when {
                evalKeys.any { c -> c.match(it) } -> {
                    it.consume()
                    GlobalScope.launch { repl.evalCurrentPrompt() }
                }
                cycleTabsForwardKombination.any { c -> c.match(it) } -> {
                    it.consume()
                    cycleForward()
                }
                cycleTabsBackwardKombination.any { c -> c.match(it) } -> {
                    it.consume()
                    cycleBackward()
                }
            }
        }

        synchronized(replIds) {
            val replId = addTab(repl)
            repl.prompt.addEventHandler(KeyEvent.KEY_PRESSED) {
                if (exitKeyCombination.any { c -> c.match(it) }) {
                    it.consume()
                    removeTab(replId)
                }
            }
        }
    }

    @Synchronized
    private fun addTab(repl: SciJavaReplFX): Int {
        val replId = smallestId(replIds.keys)
        Tab("REPL $replId", repl.node)
                .also { it.selectedProperty().addListener { _, _, new -> if (new) repl.prompt.requestFocus() } }
                .also { replIds[replId] = Pair(repl, it) }
                .also { it.onClosed = EventHandler { _ -> removeTab(it) } }
                .also { tabPane.tabs.add(it) }
                .also { tabPane.selectionModel.select(it) }
        return replId
    }

    @Synchronized
    private fun removeTab(replId: Int) = replIds.remove(replId)?.second?.let { tabPane.tabs.remove(it) }

    @Synchronized
    private fun removeTab(tab: Tab) {
        tabPane.tabs.remove(tab)
        replIds.filterValues { it.second === tab }.keys.map { it }.forEach { replIds.remove(it) }
    }

    @Synchronized
    private fun smallestId(set: Set<Int>, min: Int = 1): Int {
        var start = min
        while (set.contains(start)) ++start
        return start
    }

    private fun cycleForward() {
        tabPane.selectionModel.let { sm ->
            val tab = sm.selectedItem
            sm.selectNext()
            if (tab === sm.selectedItem)
                sm.selectFirst()
        }
    }

    private fun cycleBackward() {
        tabPane.selectionModel.let { sm ->
            val tab = sm.selectedItem
            sm.selectPrevious()
            if (tab === sm.selectedItem)
                sm.selectLast()
        }
    }
}