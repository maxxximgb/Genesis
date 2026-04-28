package com.maximg.player.util

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    val directExecutor = Executor { it.run() }
    addListener(
        {
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        },
        directExecutor
    )
    cont.invokeOnCancellation { cancel(true) }
}
