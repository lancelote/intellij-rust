/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc

import com.google.common.annotations.VisibleForTesting
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import org.rust.lang.core.macros.MACRO_LOG
import org.rust.lang.core.macros.tt.TokenTree
import org.rust.lang.core.macros.tt.TokenTreeJsonAdapter
import org.rust.openapiext.RsPathManager
import org.rust.stdext.*
import java.io.*
import java.nio.file.Path
import java.util.concurrent.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ProcMacroServer private constructor(expanderExecutable: Path) : Disposable {
    private val pool = Pool(4) {
        ProcMacroServerProcess.createAndRun(expanderExecutable) // Throws ProcessCreationException
    }

    init {
        Disposer.register(this, pool)
    }

    @Throws(ProcessCreationException::class, IOException::class, JsonParseException::class, TimeoutException::class)
    fun send(request: Request): Response {
        val io = pool.alloc() // Throws ProcessCreationException
        return try {
            io.send(request) // Throws IOException, JsonParseException, TimeoutException
        } finally {
            pool.free(io)
        }
    }

    override fun dispose() {}

    companion object {
        fun tryCreate(): ProcMacroServer? {
            val expanderExecutable = RsPathManager.nativeHelper()
            if (expanderExecutable == null || !expanderExecutable.isExecutable()) {
                return null
            }
            return createUnchecked(expanderExecutable)
        }

        @VisibleForTesting
        fun createUnchecked(expanderExecutable: Path): ProcMacroServer {
            return ProcMacroServer(expanderExecutable)
        }
    }
}

private class Pool(
    private val limit: Int,
    private val supplier: () -> ProcMacroServerProcess
) : Disposable {
    private val stack: MutableList<ProcMacroServerProcess?> = mutableListOf()
    private val stackLock: Lock = ReentrantLock()
    private val stackIsNotEmpty: Condition = stackLock.newCondition()

    @Volatile
    private var isDisposed: Boolean = false

    init {
        repeat(limit) {
            stack.add(null)
        }
    }

    fun alloc(): ProcMacroServerProcess {
        check(!isDisposed)
        val value = stackLock.withLockAndCheckingCancelled {
            while (stack.isEmpty()) {
                stackIsNotEmpty.awaitWithCheckCancelled()
            }
            stack.removeLast()
        }
        return when {
            value == null -> supply()
            value.isValid -> value
            else -> {
                Disposer.dispose(value)
                supply()
            }
        }
    }

    private fun supply(): ProcMacroServerProcess {
        val newValue = try {
            supplier()
        } catch (t: Throwable) {
            free(null)
            throw t
        }
        Disposer.register(this, newValue)
        return newValue
    }

    fun free(t: ProcMacroServerProcess?) {
        stackLock.withLock {
            stack.add(t)
            stackIsNotEmpty.signal()
        }
    }

    override fun dispose() {
        isDisposed = true
        if (stack.size != limit) {
            MACRO_LOG.error("Some processes were not freed! ${stack.size} != $limit")
        }
    }
}

/**
 * [ProcMacroServerProcess] is responsible for communicating with proc macro expander [process] and
 * manages its lifecycle.
 */
private class ProcMacroServerProcess private constructor(
    private val process: Process,
    private val stdout: BufferedReader,
    private val stdin: Writer,
    private val timeout: Long = Registry.get("org.rust.macros.proc.timeout").asInteger().toLong(),
) : Runnable, Disposable {
    private val lock = ReentrantLock()
    private val requestQueue = SynchronousQueue<Pair<Request, CompletableFuture<Response>>>()
    private val task: Future<*> = ProcessIOExecutorService.INSTANCE.submit(this)
    @Volatile
    private var isDisposed: Boolean = false

    @Throws(IOException::class, JsonParseException::class, TimeoutException::class)
    fun send(request: Request): Response {
        if (!lock.tryLock()) error("`send` must not be called from multiple threads simultaneously")
        return try {
            if (!process.isAlive) throw IOException("The process has been killed")
            val responder = CompletableFuture<Response>()
            if (!requestQueue.offer(request to responder, timeout, TimeUnit.MILLISECONDS)) {
                throw TimeoutException()
            }

            try {
                // throws TimeoutException, ExecutionException, InterruptedException
                responder.getWithCheckCanceled(timeout)
            } catch (e: ExecutionException) {
                // Unwrap exceptions from `writeAndRead` method
                throw e.cause ?: IllegalStateException("Unexpected ExecutionException without a cause", e)
            } catch (e: InterruptedException) {
                // Should not really happens
                throw ProcessCanceledException(e)
            }
        } catch (t: Throwable) {
            Disposer.dispose(this)
            throw t
        } finally {
            lock.unlock()
        }
    }

    val isValid: Boolean
        get() = !isDisposed && process.isAlive

    override fun run() {
        try {
            while (!isDisposed) {
                val (request, responder) = try {
                    requestQueue.take() // Blocks until request is available or the `task` is cancelled
                } catch (ignored: InterruptedException) {
                    return // normal shutdown
                }
                val response = try {
                    writeAndRead(request)
                } catch (e: Throwable) {
                    responder.completeExceptionally(e)
                    return
                }
                responder.complete(response)
            }
        } finally {
            process.destroyForcibly() // SIGKILL
        }
    }

    @Throws(IOException::class, JsonParseException::class)
    private fun writeAndRead(request: Request): Response {
        stdin.write(gson.toJson(request, Request::class.java))
        stdin.write("\n")
        stdin.flush()

        stdout.skipUntilJsonObject()

        return gson.fromJson(gson.newJsonReader(stdout), Response::class.java)
            ?: throw EOFException()
    }

    /**
     * A procedural macro can emit output using `println!()`.
     * Trying to skip it util `{` found
     */
    @Throws(IOException::class)
    private fun BufferedReader.skipUntilJsonObject() {
        while (true) {
            mark(1)
            val char = read()
            if (char == -1) throw EOFException()
            if (char == '{'.toInt()) {
                reset()
                break
            }
        }
    }

    override fun dispose() {
        isDisposed = true
        task.cancel(true)
        process.destroyForcibly() // SIGKILL
    }

    companion object {
        private val gson = GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Request::class.java, RequestJsonAdapter())
            .registerTypeAdapter(Response::class.java, ResponseJsonAdapter())
            .registerTypeAdapter(TokenTree::class.java, TokenTreeJsonAdapter())
            .create()

        @Throws(ProcessCreationException::class)
        fun createAndRun(expanderExecutable: Path): ProcMacroServerProcess {
            MACRO_LOG.debug { "Starting proc macro expander process $expanderExecutable" }
            val process: Process = try {
                ProcessBuilder(expanderExecutable.toString())
                    // Let a proc macro know that it is ran from intellij-rust
                    .apply { environment()["INTELLIJ_RUST"] = "true" }
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
            } catch (e: IOException) {
                throw ProcessCreationException(e)
            }

            MACRO_LOG.info("Started proc macro expander process (pid: ${process.pid()})")

            return ProcMacroServerProcess(
                process,
                BufferedReader(InputStreamReader(process.inputStream)),
                OutputStreamWriter(process.outputStream)
            )
        }
    }
}
