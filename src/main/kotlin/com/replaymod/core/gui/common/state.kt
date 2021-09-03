package com.replaymod.core.gui.common

import gg.essential.elementa.state.BasicState

class LazyState<T>(value: T) : BasicState<T>(value) {
    private var dirty = false

    override fun set(value: T) {
        if (value == valueBacker) return
        valueBacker = value
        dirty = true
    }

    fun flush() {
        if (!dirty) return
        val value = get()
        listeners.forEach { it(value) }
    }
}

class BoundedState<T>(value: T, private val constrain: (value: T) -> T) : BasicState<T>(value) {
    override fun set(value: T) {
        super.set(constrain(value))
    }
}
