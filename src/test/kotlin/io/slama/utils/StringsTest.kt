package io.slama.utils

import org.junit.jupiter.api.Test

internal class StringsTest {

    @Test
    fun splitSimpleArgs() {
        assert("this is a test".splitArgs() == listOf("this", "is", "a", "test"))
    }

    @Test
    fun splitComplexArgs() {
        assert("this \"is a\" test".splitArgs() == listOf("this", "is a", "test"))
    }

    @Test
    fun splitSimpleArgsWithEscapingDoubleQuotes() {
        assert("this \\\"is\\\" a test".splitArgs() == listOf("this", "\"is\"", "a", "test"))
    }

    @Test
    fun splitComplexArgsWithEscapingDoubleQuotes() {
        assert("this \\\"is a\\\" test".splitArgs() == listOf("this", "\"is", "a\"", "test"))
    }

    @Test
    fun splitComplexArgsWithEscapingDoubleQuotes2() {
        assert("this \"is \\\"still\\\" a\" test".splitArgs() == listOf("this", "is \"still\" a", "test"))
    }
}
