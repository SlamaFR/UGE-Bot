package io.slama.utils

fun String.pluralize(count: Number) =
    pluralize(count, null)

fun String.pluralize(count: Number, form: String?) =
    if (count.toLong() > 1) form ?: "${this}s"
    else this