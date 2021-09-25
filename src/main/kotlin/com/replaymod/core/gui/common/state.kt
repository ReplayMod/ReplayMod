package com.replaymod.core.gui.common

import gg.essential.elementa.state.State

fun <S : State<T>, T> S.lazy() = LazyState(this)
fun <S : State<T>, T> S.bounded(constrain: (value: T) -> T) = BoundedState(this, constrain)

open class DelegatingState<S : State<T>, T>(val inner: S) : State<T>() {
    init {
        @Suppress("LeakingThis")
        inner.onSetValue(this::notifyListeners)
    }

    protected open fun notifyListeners(value: T) {
        super.set(value)
    }

    override fun get(): T = inner.get()
    override fun set(value: T) = inner.set(value)
}

class LazyState<S : State<T>, T>(inner: S) : DelegatingState<S, T>(inner) {
    private var dirty = false

    override fun notifyListeners(value: T) {
        dirty = true
    }

    fun flush() {
        if (!dirty) return
        super.notifyListeners(get())
    }
}

class BoundedState<S: State<T>, T>(inner: S, private val constrain: (value: T) -> T) : DelegatingState<S, T>(inner) {
    override fun notifyListeners(value: T) = super.notifyListeners(constrain(value))
    override fun get(): T = constrain(super.get())
    override fun set(value: T) = super.set(constrain(value))
}
