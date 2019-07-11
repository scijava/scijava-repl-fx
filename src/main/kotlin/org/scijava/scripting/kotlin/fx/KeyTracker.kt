package org.scijava.scripting.kotlin.fx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import java.util.Arrays
import java.util.HashSet

class KeyTracker {

    private val activeKeys = HashSet<KeyCode>()

    private val activate = ActivateKey()

    private val deactivate = DeactivateKey()

    private val onFocusChanged = OnFocusChanged()

    fun installInto(scene: Scene) {
        scene.addEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, activate)
        scene.windowProperty().addListener(OnWindowInitListener { window ->
            window!!.focusedProperty().addListener(onFocusChanged)
        })
    }

    fun removeFrom(scene: Scene) {
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, activate)
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        if (scene.window != null)
            scene.window.focusedProperty().removeListener(onFocusChanged)
    }

    private inner class ActivateKey : EventHandler<KeyEvent> {
        override fun handle(event: KeyEvent) {
            synchronized(activeKeys) {
                activeKeys.add(event.code)
            }
        }
    }

    private inner class DeactivateKey : EventHandler<KeyEvent> {
        override fun handle(event: KeyEvent) {
            synchronized(activeKeys) {
                activeKeys.remove(event.code)
            }
        }
    }

    private inner class OnFocusChanged : ChangeListener<Boolean> {

        override fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean?, newValue: Boolean?) {
            if (!(newValue)!!)
                synchronized(activeKeys) {
                    activeKeys.clear()
                }
        }

    }

    fun areOnlyTheseKeysDown(vararg codes: KeyCode): Boolean {
        val codesHashSet = HashSet(Arrays.asList(*codes))
        synchronized(activeKeys) {
            return codesHashSet == activeKeys
        }
    }

    fun areKeysDown(vararg codes: KeyCode): Boolean {
        synchronized(activeKeys) {
            return activeKeys.containsAll(Arrays.asList(*codes))
        }
    }

    fun activeKeyCount(): Int {
        synchronized(activeKeys) {
            return activeKeys.size
        }
    }

    fun noKeysActive(): Boolean {
        return activeKeyCount() == 0
    }

}
