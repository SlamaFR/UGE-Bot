package io.slama.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TaskScheduler {
    @OptIn(DelicateCoroutinesApi::class)
    fun later(delay: Long, unit: TimeUnit, block: () -> Unit) =
        GlobalScope.launch {
            delay(unit.toMillis(delay))
            block()
        }

    @OptIn(DelicateCoroutinesApi::class)
    fun async(block: () -> Unit) =
        GlobalScope.launch {
            block()
        }
}