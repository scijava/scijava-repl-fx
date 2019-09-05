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

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.scijava.Context
import kotlin.math.sqrt

fun main() {

    val context     = Context()
    PlatformImpl.startup {}

    val tabPane = ScijavaReplFXTabs(
            context,
            bindings = *arrayOf(Pair("mySqrt", { x: Double -> sqrt(x) })))
            .also { it.createAndAddTab() }

    val root = StackPane(tabPane.node)
    Platform.runLater {
        val scene = Scene(root, 800.0, 600.0)
        val stage = Stage()
        stage.scene = scene
        stage.show()
    }

}
