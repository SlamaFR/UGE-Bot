package utils

fun String.pluralize(count: Int) =
    pluralize(count, null)

fun String.pluralize(count: Int, form: String?) =
    if (count > 1) form ?: this + "s"
    else this