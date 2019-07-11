package org.scijava.scripting.kotlin.fx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.stage.Stage
import javafx.stage.Window
import java.util.function.Consumer

class OnWindowInitListener(private val windowCheck: (Window?) -> Boolean, private val windowConsumer: (Window?) -> Unit) : ChangeListener<Window?> {

    constructor(windowConsumer: (Window?) -> Unit) : this({it != null}, windowConsumer) {}

    override fun changed(observable: ObservableValue<out Window?>, oldValue: Window?, newValue: Window?) {
        if (this.windowCheck(newValue)) {
            observable.removeListener(this)
            this.windowConsumer(newValue)
        }
    }

    companion object {

        fun doOnStageInit(stageConsumer: Consumer<Stage?>): OnSceneInitListener {
            val onWindowInit = OnWindowInitListener(
                    { window -> window != null && window is Stage },
                    { window -> stageConsumer.accept(window as Stage) }
            )
            return OnSceneInitListener { it!!.windowProperty().addListener(onWindowInit) }
        }
    }

}