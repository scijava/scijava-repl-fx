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

import javafx.geometry.Insets
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Region
import javafx.stage.Modality
import org.scijava.Context

class SciJavaReplFXDialog @JvmOverloads constructor(
        context: Context,
        bindings: Map<String, *> = mapOf<String, Any>(),
        width: Double? = null,
        height: Double? = null,
        increaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
        decreaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
        evalKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)),
        exitKeyCombination: Collection<KeyCombination> = setOf (KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)),
        createNewReplCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
        cycleTabsForwardCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN)),
        cycleTabsBackwardCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
) : Dialog<Any>() {


    @JvmOverloads constructor(
            context: Context,
            vararg bindings: Pair<String, *>,
            width: Double? = null,
            height: Double? = null,
            increaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
            decreaseFontKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_ANY)),
            evalKeys: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)),
            exitKeyCombination: Collection<KeyCombination> = setOf (KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)),
            createNewReplCombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
            cycleTabsForwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN)),
            cycleTabsBackwardKombination: Collection<KeyCombination> = setOf(KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
    ) : this(
            context,
            mapOf(*bindings),
            width,
            height,
            increaseFontKeys,
            decreaseFontKeys,
            evalKeys,
            exitKeyCombination,
            createNewReplCombination,
            cycleTabsForwardKombination,
            cycleTabsBackwardKombination)

    val tabs = ScijavaReplFXTabs(
            context = context,
            bindings = bindings,
            increaseFontKeys = increaseFontKeys,
            decreaseFontKeys =  decreaseFontKeys,
            evalKeys =  evalKeys,
            exitKeyCombination = exitKeyCombination,
            createNewReplCombination = createNewReplCombination,
            cycleTabsForwardCombination = cycleTabsForwardCombination,
            cycleTabsBackwardCombination = cycleTabsBackwardCombination)

    private var widthOnHiding: Double? = width
    private var heightOnHiding: Double? = height
    private var wasHiding = !isShowing

    init {
        // need DialogPane with custom createButtonBar to remove empty space at bottom
        dialogPane = object: DialogPane() {
            override fun createButtonBar() = Region()
        }
        dialogPane.content = tabs.node
        isResizable = true
        dialogPane.padding = Insets.EMPTY
        initModality(Modality.NONE)
        setOnShowing {
            if (!tabs.hasPrompts)
                tabs.createAndAddTab()
            wasHiding = !isShowing
        }
        setOnShown {
            if (wasHiding) {
                widthOnHiding?.let { this.width = it }
                heightOnHiding?.let { this.height = it }
            }
        }
        setOnHiding {
            widthOnHiding = this.width
            heightOnHiding = this.height
        }
        // weirdly, an invisible close button is required for the close icon to work
        // https://stackoverflow.com/questions/32048348/javafx-scene-control-dialogr-wont-close-on-pressing-x
        // https://stackoverflow.com/questions/37619885/javafx-fxml-dialog-cant-close-it-with-the-x-button?noredirect=1&lq=1
        dialogPane.buttonTypes.setAll(ButtonType.CLOSE)
        dialogPane
                .lookupButton(ButtonType.CLOSE)
                .also { it.isVisible = false }
                .also { it.managedProperty().bind(it.visibleProperty()) }

    }

}
