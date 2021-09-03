package com.replaymod.core.gui.utils

import gg.essential.elementa.UIComponent
import gg.essential.elementa.effects.Effect
import gg.essential.universal.UKeyboard

/** Marks components which can receive focus via [enableTabFocusChange]. */
private class CanReceiveFocus : Effect()

/** Limits (tab) focus changes to stay within the children of the decorated component. */
class FocusTrap : Effect()

/**
 * Enables tab-behavior for this component. Specifically this allows the user to switch away from this component to
 * instead focus the next component by pressing Tab (with Shift to go backwards) and in turn allows this component to
 * receive focus from another tab-enabled component.
 *
 * Use [FocusTrap] to limit tab switching to specific sub-tree of the component hierarchy.
 */
fun <T : UIComponent> T.enableTabFocusChange() = apply {
    enableEffect(CanReceiveFocus())
    onKeyType { _, keyCode ->
        if (keyCode == UKeyboard.KEY_TAB) {
            val backwards = UKeyboard.isShiftKeyDown()
            val nextComponent = parent.findNextFocusableComponent(this, backwards) ?: return@onKeyType
            nextComponent.grabWindowFocus()
        }
    }
}

/**
 * Returns the next focusable component after the given [fromComponent] as returned by a depth-first search of the
 * entire component hierarchy (but without actually running the entire search, and wrapping around).
 * If this or a parent component has the [FocusTrap] effect, then the search will stay within the sub-tree with the
 * respective component at its root.
 *
 * When [backwards] is `true`, the entire search is performed in reverse, returning the previous focusable component.
 *
 * When [fromComponent] is given, then this will recursively search up the tree if the next element cannot be found in
 * this subtree. If [fromComponent] is `null` (searching a sibling sub-tree of the original sub-tree), then `null` is
 * return if the next element cannot be found in this subtree.
 *
 * If the search is unsuccessful (because there is no focusable component other than [fromComponent]), then `null` is
 * returned.
 */
private fun UIComponent.findNextFocusableComponent(fromComponent: UIComponent?, backwards: Boolean): UIComponent? {
    fun findNextInRange(range: IntProgression): UIComponent? {
        for (index in range) {
            val child = children[index]
            return if (child.effects.any { it is CanReceiveFocus }) {
                child
            } else {
                child.findNextFocusableComponent(null, backwards) ?: continue
            }
        }
        return null
    }

    if (fromComponent != null) {
        val fromIndex = children.indexOf(fromComponent)
        val higherRange = (fromIndex + 1)..children.lastIndex
        val lowerRange = 0 until fromIndex

        // first check siblings in the respective direction
        findNextInRange(if (backwards) lowerRange.reversed() else higherRange)?.let { return it }

        // then go one level up and check siblings there, recursively
        if (parent != this && effects.none { it is FocusTrap }) {
            parent.findNextFocusableComponent(this, backwards)?.let { return it }
        }

        // and finally, back on this level, check the other half of siblings
        findNextInRange(if (backwards) higherRange.reversed() else lowerRange)?.let { return it }
    } else {
        // check all children in the respective order
        findNextInRange(if (backwards) children.indices.reversed() else children.indices)?.let { return it }
    }

    // looped all the way round and still haven't found anything
    return null
}
