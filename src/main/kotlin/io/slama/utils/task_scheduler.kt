package io.slama.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TaskScheduler {
    fun later(delay: Long, unit: TimeUnit, block: () -> Unit): Job =
        CoroutineScope(Dispatchers.IO).launch {
            delay(unit.toMillis(delay))
            block()
        }

    fun async(block: () -> Unit): Job =
        CoroutineScope(Dispatchers.IO).launch {
            block()
        }

    fun repeat(period: Long, unit: TimeUnit, block: () -> Boolean): Job =
        CoroutineScope(Dispatchers.IO).launch {
            while (block()) {
                delay(unit.toMillis(period))
            }
        }
}
