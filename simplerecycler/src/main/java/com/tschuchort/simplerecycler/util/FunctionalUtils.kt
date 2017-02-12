package com.tschuchort.simplerecycler.util

import android.util.Pair
import android.util.SparseArray

internal fun <T> T.transformIf(predicate: Boolean, f: T.()->T): T
		= if(predicate) f() else this

internal fun <E> Collection<E>.forEachAndThen(action: (E) -> Unit): Collection<E> {
	forEach(action)
	return this
}

internal fun <V> Collection<Pair<Int, V>>.toSparseArray(): SparseArray<V> {
	val sArr = SparseArray<V>(this.size)
	forEach { sArr.put(it.first, it.second) }
	return sArr
}