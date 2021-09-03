package com.replaymod.core.gui.utils

import gg.essential.elementa.UIComponent
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State

fun <T> State<T>.onSetValueAndNow(block: (value: T) -> Unit) = onSetValue(block).also { block(get()) }

fun <T> UIComponent.pollingState(initialValue: T? = null, getter: () -> T): State<T> {
    val state = BasicState(initialValue ?: getter())
    enableEffect(object : Effect() {
        override fun animationFrame() {
            state.set(getter())
        }
    })
    return state
}

fun <T : UIComponent, S> T.bindStateTransition(
    state: State<S>,
    update: T.(done: () -> Unit, oldState: S, newState: S) -> Unit,
) = apply {
    state.bindTransition { done, oldState, newState -> update(done, oldState, newState) }
}

fun <S> State<S>.bindTransition(update: (done: () -> Unit, oldState: S, newState: S) -> Unit) {
    var activeState = get()
    var pendingState: S? = null
    var inProgress = false

    fun onStateChanged(newState: S) {
        if (inProgress) {
            pendingState = newState
            return
        }

        if (newState == activeState) {
            return
        }

        val oldState = activeState
        activeState = newState

        inProgress = true
        update({
            inProgress = false
            pendingState?.also { pendingState = null }?.let(::onStateChanged)
        }, oldState, newState)
    }
    onSetValue(::onStateChanged)
}
