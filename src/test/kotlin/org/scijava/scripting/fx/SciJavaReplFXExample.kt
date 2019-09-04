package org.scijava.scripting.fx

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.scijava.Context

fun main() {

    val context     = Context()
    PlatformImpl.startup {}

    val tabPane = ScijavaReplFXTabs(context).also { it.createAndAddTab() }

    val root = StackPane(tabPane.node)
    Platform.runLater {
        val scene = Scene(root, 800.0, 600.0)
        val stage = Stage()
        stage.scene = scene
        stage.show()
    }

}