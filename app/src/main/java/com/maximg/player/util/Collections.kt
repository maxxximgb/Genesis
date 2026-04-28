package com.maximg.player.util

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(if (to > from) to - 1 else to, item)
}
