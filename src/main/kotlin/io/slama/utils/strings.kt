package io.slama.utils

import java.util.regex.Pattern


fun String.pluralize(count: Number) =
    pluralize(count, null)

fun String.pluralize(count: Number, form: String?) =
    if (count.toLong() > 1) form ?: "${this}s"
    else this

fun String.splitArgs(): List<String> {
    val matchList = ArrayList<String>()
    val regex = Pattern.compile("""(?:\\"|[^\s"])+|"(?:\\"|[^"])*"""")
    val regexMatcher = regex.matcher(this)
    while (regexMatcher.find()) {
        matchList.add(
            regexMatcher
                .group(0)
                .replace("(?<!\\\\)\"".toRegex(), "")
                .replace("\\\\\"".toRegex(), "\"")
        )
    }
    return matchList
}
