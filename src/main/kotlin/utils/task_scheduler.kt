package utils

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

object TaskScheduler {
    @OptIn(DelicateCoroutinesApi::class) // TODO: Fix opt in compiler args
    fun later(delay: Long, unit: TimeUnit, block: () -> Unit) =
        GlobalScope.launch {
            delay(unit.toMillis(delay))
            block()
        }

    @OptIn(DelicateCoroutinesApi::class) // TODO: Fix opt in compiler args
    fun async(block: () -> Unit) =
        GlobalScope.launch {
            block()
        }
}