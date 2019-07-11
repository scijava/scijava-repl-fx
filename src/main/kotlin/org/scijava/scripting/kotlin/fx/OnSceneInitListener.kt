package org.scijava.scripting.kotlin.fx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Scene

class OnSceneInitListener(private val sceneCheck: (Scene?) -> Boolean, private val sceneConsumer: (Scene?) -> Unit) : ChangeListener<Scene?> {

    constructor(sceneConsumer: (Scene?) -> Unit) : this({ it != null }, sceneConsumer)

    override fun changed(observable: ObservableValue<out Scene?>, oldValue: Scene?, newValue: Scene?) {
        if (sceneCheck(newValue)) {
            observable.removeListener(this)
            sceneConsumer(newValue)
        }
    }

}