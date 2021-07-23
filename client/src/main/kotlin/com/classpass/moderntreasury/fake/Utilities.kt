package com.classpass.moderntreasury.fake

/**
 * this matches other if all for all keys in this map, the value exists and matches in other.
 *
 * Note that (a matches b) does NOT imply (b matches a).
 */
internal infix fun <K, V> Map<K, V>.matches(other: Map<K, V>) =
    this.all { (k, _) -> other.containsKey(k) && this[k] == other[k] }
