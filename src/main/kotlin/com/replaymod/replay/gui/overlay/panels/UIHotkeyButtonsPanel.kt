package com.replaymod.replay.gui.overlay.panels

import com.replaymod.core.KeyBindingRegistry
import com.replaymod.core.ReplayMod
import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.common.scrollbar.UIFlatScrollBar
import com.replaymod.core.gui.common.UITexture
import com.replaymod.core.gui.common.elementa.UIScrollComponent
import com.replaymod.core.gui.utils.addTooltip
import com.replaymod.core.gui.utils.pollingState
import com.replaymod.core.utils.i18n
import com.replaymod.core.utils.*
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.resource.language.I18n
import java.awt.Color

class UIHotkeyButtonsPanel(
    keyBindings: Collection<KeyBindingRegistry.Binding> = ReplayMod.instance.keyBindingRegistry.bindings.values,
) : UIToggleablePanel() {
    init {
        toggleButton.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(0, 120, 20, 20))

        constrain {
            width = 200.pixels
            height = 100.percent - 80.pixels
        }
    }

    val scrollContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = 4.pixels
        height = 100.percent - 20.pixels - 8.pixels
        width = 100.percent - 8.pixels
    } childOf this

    val scrollComponent by UIScrollComponent().constrain {
        width = 100.percent - 10.pixels
        height = 100.percent
    }.apply {
        for (keyBinding in keyBindings.sortedBy { I18n.translate(it.name) }) {
            addChild(Row(keyBinding))
        }
    } childOf scrollContainer

    val scrollBar by UIFlatScrollBar(transparent = true).constrain {
        x = 0.pixels(alignOpposite = true)
        width = 6.pixels
    }.apply {
        scrollComponent.setVerticalScrollBarComponent(grip)
    } childOf scrollContainer

    val filter by UITextInput("Click to filter").constrain {
        x = 10.pixels
        y = (2 + 5).pixels(alignOpposite = true)
        width = 100.percent - 10.pixels - 20.pixels - 2.pixels
        height = 10.pixels
    }.apply {
        // When the panel is opened, immediately grant focus and clear it (so we can get straight to typing)
        open.onSetValue {
            if (it) {
                grabWindowFocus()
                setText("")
            }
        }
    }.onUpdate { search ->
        scrollComponent.filterChildren { (it as Row).label.getText().contains(search, ignoreCase = true) }
    }.onActivate {
        val row = scrollComponent.childrenOfType<Row>().singleOrNull() ?: return@onActivate
        open.set(false)
        row.keyBinding.trigger()
    }.onMouseClick {
        grabWindowFocus()
    } childOf this

    inner class Row(val keyBinding: KeyBindingRegistry.Binding) : UIContainer() {
        val button by UIButton().constrain {
            y = CenterConstraint()
            width = ChildBasedSizeConstraint().coerceAtLeast(10.pixels) + 10.pixels
            height = 20.pixels
        }.label {
            bindText(pollingState { if (keyBinding.isBound) keyBinding.boundKey else "" })
            constrain {
                val orgColor = color
                color = basicColorConstraint {
                    if (keyBinding.isAutoActivating) Color.GREEN else orgColor.getColor(it)
                }
            }
        }.onMouseClick {
            it.stopImmediatePropagation()

            if (keyBinding.supportsAutoActivation() && Screen.hasControlDown()) {
                keyBinding.isAutoActivating = !keyBinding.isAutoActivating
            } else {
                keyBinding.trigger()
            }
        }.apply {
            if (keyBinding.supportsAutoActivation()) {
                addTooltip {
                    addLine("replaymod.gui.ingame.autoactivating".i18n())
                    addLine {
                        bindText(pollingState {
                            if (keyBinding.isAutoActivating) {
                                "replaymod.gui.ingame.autoactivating.disable".i18n()
                            } else {
                                "replaymod.gui.ingame.autoactivating.enable".i18n()
                            }
                        })
                    }
                }
            }
        }

        val label = UIWrappedText().constrain {
            x = SiblingConstraint(padding = 4f)
            y = CenterConstraint()
            width = FillConstraint(useSiblings = false)
        }.setText(keyBinding.name.i18n())

        init {
            constrain {
                width = 100.percent
                height = 25.pixels
                y = SiblingConstraint()
            }
            addChildren(button, label)
        }
    }
}