package com.replaymod.core.utils

import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f
import net.minecraft.client.resource.language.I18n
import java.util.*

fun String.i18n(vararg args: String): String = I18n.translate(this, *args)

val <T> Optional<T>?.orNull get() = this?.orElse(null)

fun <A, B> Pair<A, B>?.transpose() = this ?: Pair(null, null)

val <L, M, R> org.apache.commons.lang3.tuple.Triple<L, M, R>.kt: Triple<L, M, R>
    get() = Triple(left, middle, right)

fun Triple<Float, Float, Float>.toDouble() = Triple(first.toDouble(), second.toDouble(), third.toDouble())

fun Triple<Double, Double, Double>.toVector3f() = Vector3f(first.toFloat(), second.toFloat(), third.toFloat())

inline fun <T, K, V> Iterable<T>.associateNotNull(transform: (T) -> Pair<K, V>?): Map<K, V> {
    val destination = LinkedHashMap<K, V>((this as? Collection<T>)?.size ?: 16)
    for (element in this) {
        destination += transform(element) ?: continue
    }
    return destination
}

fun <T> MutableSet<T>.toggle(element: T) = if (element in this) remove(element) else add(element)
